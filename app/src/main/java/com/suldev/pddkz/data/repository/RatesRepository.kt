package com.suldev.pddkz.data.repository

import com.suldev.pddkz.data.network.NbkRatesApi
import com.suldev.pddkz.data.storage.PreferencesStorage
import com.suldev.pddkz.domain.model.NbkRates
import com.suldev.pddkz.domain.model.RatesState
import com.suldev.pddkz.domain.model.RatesStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

/** Кэш → сеть. Пока нет ни кэша, ни ответа — фолбэк-курсы из дефолтов [NbkRates]. */
class RatesRepository(
    private val api: NbkRatesApi,
    private val storage: PreferencesStorage,
) {
    private val _state = MutableStateFlow(RatesState())
    val state: StateFlow<RatesState> = _state.asStateFlow()

    /** Первичная загрузка и pull-to-refresh: сразу показывает кэш, затем идёт в сеть. */
    suspend fun refresh() {
        _state.value = _state.value.copy(status = RatesStatus.Loading)
        val cached = storage.cachedRates()
        _state.value = RatesState(cached ?: _state.value.rates, RatesStatus.Loading)

        val fresh = try {
            api.fetch().let { NbkRates(cnyKzt = it.cny, usdKzt = it.usd, date = it.date, source = it.source) }
        } catch (e: IOException) {
            val status = cached?.date?.let { RatesStatus.Cached(it) } ?: RatesStatus.Offline
            _state.value = _state.value.copy(status = status)
            return
        }

        _state.value = RatesState(fresh, RatesStatus.Fresh(fresh.date.orEmpty()))
        try {
            storage.saveRates(fresh)
        } catch (_: IOException) {
            // Не записался кэш — свежие курсы на экране всё равно верные.
        }
    }
}
