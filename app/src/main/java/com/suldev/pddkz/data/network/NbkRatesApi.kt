package com.suldev.pddkz.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class NbkRatesDto(
    val usd: Double,
    val cny: Double,
    val date: String,
    val source: String = "",
)

class NbkRatesApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
) {
    /** Бросает [IOException] при любой неудаче: сеть, HTTP ≠ 2xx, битый или бессмысленный JSON. */
    suspend fun fetch(): NbkRatesDto = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(URL).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            parse(response.body?.string().orEmpty())
        }
    }

    companion object {
        const val URL = "https://pdd-kz-calc.netlify.app/api/rates"

        private val json = Json { ignoreUnknownKeys = true }

        fun parse(body: String): NbkRatesDto {
            val dto = try {
                json.decodeFromString<NbkRatesDto>(body)
            } catch (e: IllegalArgumentException) { // SerializationException — его подкласс
                throw IOException("Невалидный JSON курсов", e)
            }
            // Нулевой или отрицательный курс сломал бы расчёт — считаем ответ невалидным.
            if (!(dto.usd.isFinite() && dto.usd > 0 && dto.cny.isFinite() && dto.cny > 0)) {
                throw IOException("Некорректные курсы: usd=${dto.usd}, cny=${dto.cny}")
            }
            return dto
        }
    }
}
