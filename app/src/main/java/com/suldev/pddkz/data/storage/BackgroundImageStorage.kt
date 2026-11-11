package com.suldev.pddkz.data.storage

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.suldev.pddkz.domain.model.BackgroundStyle
import com.suldev.pddkz.ui.components.BACKGROUNDS_DIR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Хранилище пользовательского фона в `filesDir/backgrounds/`.
 *
 * Поток сохранения (§4.1):
 * EXIF-поворот → `inSampleSize` (максимальная сторона ≤ 2048 px) →
 * атомарная запись JPEG `q = 88` под [Mutex] → удаление предыдущих файлов →
 * сохранение только имени файла в [PreferencesStorage].
 */
class BackgroundImageStorage(
    private val backgroundsDir: File,
    private val storage: PreferencesStorage,
    private val imageProcessor: ImageProcessor = AndroidImageProcessor,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    constructor(context: Context, storage: PreferencesStorage) : this(
        backgroundsDir = File(context.filesDir, BACKGROUNDS_DIR),
        storage = storage,
    )

    private val mutex = Mutex()

    interface ImageProcessor {
        fun processToJpeg(bytes: ByteArray, destination: File)
    }

    suspend fun saveFromUri(contentResolver: ContentResolver, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                saveFromStreamInternal(stream)
            } ?: throw IOException("Не удалось открыть выбранное изображение")
        }
    }

    suspend fun saveFromStream(inputStream: InputStream): Result<String> = withContext(Dispatchers.IO) {
        runCatching { saveFromStreamInternal(inputStream) }
    }

    private suspend fun saveFromStreamInternal(inputStream: InputStream): String = mutex.withLock {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) throw IllegalArgumentException("Пустой файл изображения")
        backgroundsDir.mkdirs()

        val fileName = nextFileName()
        val tempFile = File(backgroundsDir, "$fileName.tmp")
        val targetFile = File(backgroundsDir, fileName)

        try {
            imageProcessor.processToJpeg(bytes, tempFile)
            if (!tempFile.exists() || tempFile.length() == 0L) {
                throw IOException("Не удалось сохранить изображение фона")
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            deleteOtherFiles(keepName = fileName)
            storage.setBackgroundFileName(fileName)
            storage.updateSettings { it.copy(backgroundStyle = BackgroundStyle.IMAGE) }
            fileName
        } catch (t: Throwable) {
            tempFile.delete()
            fallbackIfNoValidFile()
            throw t
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            deleteOtherFiles(keepName = null)
            storage.setBackgroundFileName(null)
            storage.updateSettings {
                if (it.backgroundStyle == BackgroundStyle.IMAGE) {
                    it.copy(backgroundStyle = BackgroundStyle.PLAIN)
                } else {
                    it
                }
            }
        }
    }

    /** Если в настройках выбран [BackgroundStyle.IMAGE], а файла нет или он пуст — откатывает к `PLAIN`. */
    suspend fun validateOrFallback() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val settings = storage.settings.first()
            val currentName = storage.backgroundFileName.first()
            if (currentName.isNullOrBlank()) {
                if (settings.backgroundStyle == BackgroundStyle.IMAGE) {
                    deleteOtherFiles(keepName = null)
                }
                return@withLock
            }
            val file = File(backgroundsDir, currentName)
            if (!file.isFile || file.length() == 0L) {
                fallbackIfNoValidFile()
            }
        }
    }

    private suspend fun fallbackIfNoValidFile() {
        val previousName = storage.backgroundFileName.first()
        val hasPrevious = !previousName.isNullOrBlank() &&
            File(backgroundsDir, previousName).let { it.isFile && it.length() > 0L }
        if (!hasPrevious) {
            deleteOtherFiles(keepName = null)
            storage.setBackgroundFileName(null)
            storage.updateSettings {
                if (it.backgroundStyle == BackgroundStyle.IMAGE) {
                    it.copy(backgroundStyle = BackgroundStyle.PLAIN)
                } else {
                    it
                }
            }
        }
    }

    private fun deleteOtherFiles(keepName: String?) {
        backgroundsDir.listFiles()?.forEach { file ->
            if (file.name != keepName) file.delete()
        }
    }

    private fun nextFileName(): String {
        val base = nowMillis()
        var candidate = "custom_bg_$base.jpg"
        var suffix = 1
        while (File(backgroundsDir, candidate).exists()) {
            candidate = "custom_bg_${base}_$suffix.jpg"
            suffix++
        }
        return candidate
    }

    companion object {
        const val MAX_DIMENSION_PX = 2048
        const val JPEG_QUALITY = 88

        fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int = MAX_DIMENSION_PX): Int {
            if (width <= 0 || height <= 0 || maxDimension <= 0) return 1
            var sampleSize = 1
            val maxSide = maxOf(width, height)
            while (maxSide / sampleSize > maxDimension) {
                sampleSize *= 2
            }
            return sampleSize
        }

        internal fun exifMatrix(orientation: Int): Matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    postScale(-1f, 1f)
                    postRotate(-90f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    postScale(-1f, 1f)
                    postRotate(90f)
                }
            }
        }
    }
}

private object AndroidImageProcessor : BackgroundImageStorage.ImageProcessor {
    override fun processToJpeg(bytes: ByteArray, destination: File) {
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalArgumentException("Не удалось прочитать размеры изображения")
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = BackgroundImageStorage.calculateInSampleSize(bounds.outWidth, bounds.outHeight)
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
            ?: throw IllegalArgumentException("Не удалось декодировать изображение")

        val matrix = BackgroundImageStorage.exifMatrix(orientation)
        val oriented = if (!matrix.isIdentity) {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                if (it != decoded) decoded.recycle()
            }
        } else {
            decoded
        }

        val maxSide = maxOf(oriented.width, oriented.height)
        val finalBitmap = if (maxSide > BackgroundImageStorage.MAX_DIMENSION_PX) {
            val scale = BackgroundImageStorage.MAX_DIMENSION_PX.toFloat() / maxSide.toFloat()
            val targetW = (oriented.width * scale).toInt().coerceAtLeast(1)
            val targetH = (oriented.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(oriented, targetW, targetH, true).also {
                if (it != oriented) oriented.recycle()
            }
        } else {
            oriented
        }

        try {
            FileOutputStream(destination).use { out ->
                if (!finalBitmap.compress(Bitmap.CompressFormat.JPEG, BackgroundImageStorage.JPEG_QUALITY, out)) {
                    throw IOException("Не удалось записать JPEG")
                }
            }
        } finally {
            finalBitmap.recycle()
        }
    }
}
