package com.suldev.pddkz

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.suldev.pddkz.data.storage.BackgroundImageStorage
import com.suldev.pddkz.data.storage.PreferencesStorage
import com.suldev.pddkz.domain.model.AppSettings
import com.suldev.pddkz.domain.model.BackgroundStyle
import com.suldev.pddkz.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

class BackgroundImageStorageTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO)

    @After
    fun tearDown() = scope.cancel()

    private fun newPreferencesStorage(name: String = "pdd.preferences_pb"): PreferencesStorage {
        val file = File(tmp.root, name)
        return PreferencesStorage(PreferenceDataStoreFactory.create(scope = scope) { file })
    }

    @Test
    fun `inSampleSize scales largest dimension down to 2048 px or less`() {
        assertEquals(1, BackgroundImageStorage.calculateInSampleSize(1920, 1080))
        assertEquals(1, BackgroundImageStorage.calculateInSampleSize(2048, 2048))
        assertEquals(2, BackgroundImageStorage.calculateInSampleSize(4096, 3072))
        assertEquals(4, BackgroundImageStorage.calculateInSampleSize(8000, 6000))
    }

    @Test
    fun `save replaces previous background files and clear leaves directory empty`() = runBlocking {
        val bgDir = tmp.newFolder("backgrounds")
        val prefs = newPreferencesStorage()
        val clock = AtomicLong(1000L)
        val processor = object : BackgroundImageStorage.ImageProcessor {
            override fun processToJpeg(bytes: ByteArray, destination: File) {
                destination.writeBytes(bytes)
            }
        }
        val bgStorage = BackgroundImageStorage(bgDir, prefs, processor) { clock.getAndIncrement() }

        val first = bgStorage.saveFromStream(ByteArrayInputStream(byteArrayOf(1, 2, 3))).getOrThrow()
        assertEquals(listOf(first), bgDir.list()?.toList())
        assertEquals(first, prefs.backgroundFileName.first())
        assertEquals(BackgroundStyle.IMAGE, prefs.settings.first().backgroundStyle)

        val second = bgStorage.saveFromStream(ByteArrayInputStream(byteArrayOf(4, 5, 6))).getOrThrow()
        assertEquals(listOf(second), bgDir.list()?.toList())
        assertEquals(second, prefs.backgroundFileName.first())

        bgStorage.clear()
        assertTrue(bgDir.list().orEmpty().isEmpty())
        assertNull(prefs.backgroundFileName.first())
        assertEquals(BackgroundStyle.PLAIN, prefs.settings.first().backgroundStyle)
    }

    @Test
    fun `decode failure cleans temp files and falls back to plain when no previous photo exists`() = runBlocking {
        val bgDir = tmp.newFolder("backgrounds")
        val prefs = newPreferencesStorage()
        prefs.updateSettings { it.copy(backgroundStyle = BackgroundStyle.IMAGE) }

        val failingProcessor = object : BackgroundImageStorage.ImageProcessor {
            override fun processToJpeg(bytes: ByteArray, destination: File) {
                destination.writeBytes(byteArrayOf(0))
                throw IOException("corrupt")
            }
        }
        val bgStorage = BackgroundImageStorage(bgDir, prefs, failingProcessor) { 2000L }

        val result = bgStorage.saveFromStream(ByteArrayInputStream(byteArrayOf(9, 9)))
        assertTrue(result.isFailure)
        assertTrue(bgDir.list().orEmpty().isEmpty())
        assertNull(prefs.backgroundFileName.first())
        assertEquals(BackgroundStyle.PLAIN, prefs.settings.first().backgroundStyle)
    }

    @Test
    fun `validateOrFallback reverts missing image file to plain`() = runBlocking {
        val bgDir = tmp.newFolder("backgrounds")
        val prefs = newPreferencesStorage()
        prefs.setBackgroundFileName("missing_bg.jpg")
        prefs.updateSettings { it.copy(backgroundStyle = BackgroundStyle.IMAGE) }

        val bgStorage = BackgroundImageStorage(bgDir, prefs)
        bgStorage.validateOrFallback()

        assertNull(prefs.backgroundFileName.first())
        assertEquals(BackgroundStyle.PLAIN, prefs.settings.first().backgroundStyle)
    }

    @Test
    fun `settings survive reopening datastore`() = runBlocking {
        val storeFile = File(tmp.root, "persist.preferences_pb")
        val firstScope = CoroutineScope(Dispatchers.IO)
        val firstPrefs = PreferencesStorage(PreferenceDataStoreFactory.create(scope = firstScope) { storeFile })
        firstPrefs.updateSettings {
            it.copy(
                themeMode = ThemeMode.DARK,
                backgroundStyle = BackgroundStyle.MESH,
                reduceTransparency = true,
                dimOpacity = 0.55f,
            )
        }
        firstScope.cancel()
        firstScope.coroutineContext[Job]?.join()

        val secondPrefs = PreferencesStorage(PreferenceDataStoreFactory.create(scope = scope) { storeFile })
        val restored = secondPrefs.settings.first()
        assertEquals(
            AppSettings(
                themeMode = ThemeMode.DARK,
                backgroundStyle = BackgroundStyle.MESH,
                reduceTransparency = true,
                dimOpacity = 0.55f,
            ),
            restored,
        )
    }
}
