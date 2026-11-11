package com.suldev.pddkz.ui.components

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.suldev.pddkz.domain.model.BackgroundStyle
import com.suldev.pddkz.ui.theme.PddTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Каталог пользовательских фонов внутри `filesDir`. */
const val BACKGROUNDS_DIR = "backgrounds"

/**
 * Декодирует пользовательское фото один раз на имя файла на IO-потоке,
 * чтобы оба источника стекла делили один [ImageBitmap] без блокировки UI.
 */
@Composable
fun rememberBackgroundImage(
    style: BackgroundStyle,
    backgroundFileName: String?,
    onDecodeFailed: (() -> Unit)? = null,
): ImageBitmap? {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(initialValue = null, style, backgroundFileName) {
        if (style != BackgroundStyle.IMAGE || backgroundFileName.isNullOrBlank()) {
            value = null
            return@produceState
        }
        val decoded = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(File(context.filesDir, BACKGROUNDS_DIR), backgroundFileName)
                if (!file.isFile) return@runCatching null
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            }.getOrNull()
        }
        value = decoded
        if (decoded == null) onDecodeFailed?.invoke()
    }
    return image
}

/**
 * Фон приложения. Вызывается дважды (в каждом источнике стекла) с одинаковыми аргументами.
 * Если фото не передано или не декодировалось — откат к однотону.
 */
@Composable
fun AppBackground(
    style: BackgroundStyle,
    image: ImageBitmap?,
    dimOpacity: Float,
    modifier: Modifier = Modifier,
) {
    val c = PddTheme.colors
    com.suldev.ui.SulAppBackground(
        baseColor = c.background,
        isDark = c.isDark,
        modifier = modifier,
        mesh = style == BackgroundStyle.MESH,
        image = if (style == BackgroundStyle.IMAGE) image else null,
        dimOpacity = dimOpacity,
    )
}
