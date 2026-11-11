package com.suldev.pddkz

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.materialfluid.compose.MaterialFluidMaterial
import com.suldev.pddkz.data.network.NbkRatesApi
import com.suldev.pddkz.data.repository.RatesRepository
import com.suldev.pddkz.data.storage.PreferencesStorage
import com.suldev.pddkz.domain.model.AppSettings
import com.suldev.pddkz.domain.model.BackgroundStyle
import com.suldev.pddkz.domain.model.NbkRates
import com.suldev.pddkz.domain.model.RatesStatus
import com.suldev.pddkz.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class NbkRatesParsingTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO)
    private val storage by lazy {
        PreferencesStorage(PreferenceDataStoreFactory.create(scope = scope) { tmp.root.resolve("pdd.preferences_pb") })
    }

    @After
    fun tearDown() = scope.cancel()

    private val validJson =
        """{"usd": 444.76, "cny": 66.68, "date": "12.11.2026", "source": "Национальный Банк РК"}"""

    /** API без сети: перехватчик отдаёт заготовленный ответ или бросает ошибку. */
    private fun api(code: Int = 200, body: String = validJson, fail: Boolean = false) = NbkRatesApi(
        OkHttpClient.Builder().addInterceptor { chain ->
            if (fail) throw IOException("нет сети")
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("test")
                .body(body.toResponseBody())
                .build()
        }.build(),
    )

    // --- Парсинг ---

    @Test
    fun `valid json`() {
        val dto = NbkRatesApi.parse(validJson)

        assertEquals(444.76, dto.usd, 0.0)
        assertEquals(66.68, dto.cny, 0.0)
        assertEquals("12.11.2026", dto.date)
        assertEquals("Национальный Банк РК", dto.source)
    }

    @Test
    fun `unknown fields are ignored`() {
        assertEquals(66.68, NbkRatesApi.parse("""{"usd": 444.76, "cny": 66.68, "date": "12.11.2026", "eur": 500}""").cny, 0.0)
    }

    @Test
    fun `broken or meaningless json is rejected`() {
        listOf(
            "",
            "<html>502</html>",
            """{"usd": 444.76""",
            """{"usd": 444.76, "date": "12.11.2026"}""", // нет cny
            """{"usd": "abc", "cny": 66.68, "date": "12.11.2026"}""",
            """{"usd": 0, "cny": 66.68, "date": "12.11.2026"}""",
            """{"usd": 444.76, "cny": -1, "date": "12.11.2026"}""",
        ).forEach { body ->
            assertThrows(body, IOException::class.java) { NbkRatesApi.parse(body) }
        }
    }

    @Test
    fun `http error is rejected even with valid body`() {
        assertThrows(IOException::class.java) { runBlocking { api(code = 500).fetch() } }
        assertThrows(IOException::class.java) { runBlocking { api(code = 404).fetch() } }
    }

    // --- Репозиторий: кэш, фолбэк и четыре метки ---

    @Test
    fun `starts with fallback rates and loading label`() {
        val state = RatesRepository(api(), storage).state.value

        assertEquals(NbkRates(cnyKzt = 68.11, usdKzt = 453.71), state.rates)
        assertEquals("загрузка…", state.status.label)
    }

    @Test
    fun `success shows fresh rates and caches them`() = runBlocking {
        val repo = RatesRepository(api(), storage)
        repo.refresh()

        val expected = NbkRates(66.68, 444.76, "12.11.2026", "Национальный Банк РК")
        assertEquals(expected, repo.state.value.rates)
        assertEquals("на 12.11.2026", repo.state.value.status.label)
        assertEquals(expected, storage.cachedRates())
    }

    @Test
    fun `error without cache keeps fallback`() = runBlocking {
        listOf(api(fail = true), api(code = 503), api(body = "oops")).forEach { api ->
            val repo = RatesRepository(api, storage)
            repo.refresh()

            assertEquals(NbkRates(), repo.state.value.rates)
            assertEquals("нет связи с НБ РК", repo.state.value.status.label)
        }
        assertNull(storage.cachedRates())
    }

    @Test
    fun `error with cache shows cached rates`() = runBlocking {
        RatesRepository(api(), storage).refresh()

        // Новый репозиторий — как после перезапуска приложения без сети.
        val repo = RatesRepository(api(fail = true), storage)
        repo.refresh()

        assertEquals(66.68, repo.state.value.rates.cnyKzt, 0.0)
        assertEquals(444.76, repo.state.value.rates.usdKzt, 0.0)
        assertEquals(RatesStatus.Cached("12.11.2026"), repo.state.value.status)
        assertEquals("кэш от 12.11.2026 · нет связи с НБ РК", repo.state.value.status.label)
    }

    // --- Настройки в том же DataStore ---

    @Test
    fun `settings round trip and clamp`() = runBlocking {
        assertEquals(AppSettings(), storage.settings.first())

        storage.updateSettings {
            it.copy(
                themeMode = ThemeMode.DARK,
                material = MaterialFluidMaterial.Frosted,
                backgroundStyle = BackgroundStyle.IMAGE,
                reduceTransparency = true,
                dimOpacity = 0.99f,
            )
        }
        storage.setBackgroundFileName("bg_1.jpg")

        assertEquals(
            AppSettings(ThemeMode.DARK, MaterialFluidMaterial.Frosted, BackgroundStyle.IMAGE, true, AppSettings.DIM_MAX),
            storage.settings.first(),
        )
        assertEquals("bg_1.jpg", storage.backgroundFileName.first())

        storage.setBackgroundFileName(null)
        assertNull(storage.backgroundFileName.first())
    }
}
