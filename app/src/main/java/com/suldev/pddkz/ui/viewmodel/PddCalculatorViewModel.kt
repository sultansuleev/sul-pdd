package com.suldev.pddkz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suldev.pddkz.data.repository.RatesRepository
import com.suldev.pddkz.domain.model.CalculationResult
import com.suldev.pddkz.domain.model.CalculatorInput
import com.suldev.pddkz.domain.model.DisplayCurrency
import com.suldev.pddkz.domain.model.RatesState
import com.suldev.pddkz.domain.model.RatesStatus
import com.suldev.pddkz.domain.parseDecimal
import com.suldev.pddkz.domain.parseQty
import com.suldev.pddkz.domain.usecase.CalculateDeliveryUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Field { ITEM_YUAN, ITEM_WEIGHT, ITEM_QTY, VAT_PCT, FEE_PCT, KAZPOST_DAYS, CARGO_RATE, CARGO_DAYS }

/** Целое показываем без `.0`: `180`, но `1.2`. */
fun Double.toFieldText(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

/** Сырой текст полей ввода — ровно то, что набрал пользователь. */
data class CalculatorFields(
    val currency: DisplayCurrency = DisplayCurrency.KZT,
    val text: Map<Field, String> = Defaults,
) {
    operator fun get(field: Field): String = text[field].orEmpty()

    companion object {
        private val Defaults: Map<Field, String> = CalculatorInput().let {
            mapOf(
                Field.ITEM_YUAN to it.itemYuan.toFieldText(),
                Field.ITEM_WEIGHT to it.itemWeightKg.toFieldText(),
                Field.ITEM_QTY to it.itemQty.toString(),
                Field.VAT_PCT to it.kazpostVatPct.toFieldText(),
                Field.FEE_PCT to it.kazpostFeePct.toFieldText(),
                Field.KAZPOST_DAYS to it.kazpostDays,
                Field.CARGO_RATE to it.cargoRateUsdPerKg.toFieldText(),
                Field.CARGO_DAYS to it.cargoDays,
            )
        }
    }
}

/** Разобранный ввод: значения для расчёта + сообщения под полями. */
data class ParsedInput(
    val input: CalculatorInput,
    val errors: Map<Field, String>,
    /** Нейтральная подсказка под полем количества, когда оно пустое. */
    val qtyHint: String?,
)

/**
 * Пустое числовое поле — это 0 без ошибки (человек ещё печатает), нечисло — 0 с ошибкой.
 * Количество: пустое → 1 с подсказкой, меньше 1 или дробное → 1 с ошибкой.
 * Пустой срок → подпись по умолчанию.
 */
fun CalculatorFields.parse(): ParsedInput {
    val errors = mutableMapOf<Field, String>()
    fun number(field: Field): Double {
        val raw = this[field]
        if (raw.isBlank()) return 0.0
        return parseDecimal(raw) ?: 0.0.also { errors[field] = "Введите число от 0" }
    }

    val qtyRaw = this[Field.ITEM_QTY]
    val qty = parseQty(qtyRaw)
    if (qty == null && qtyRaw.isNotBlank()) errors[Field.ITEM_QTY] = "Минимум 1 шт"

    return ParsedInput(
        input = CalculatorInput(
            currency = currency,
            itemYuan = number(Field.ITEM_YUAN),
            itemWeightKg = number(Field.ITEM_WEIGHT),
            itemQty = qty ?: 1,
            kazpostVatPct = number(Field.VAT_PCT),
            kazpostFeePct = number(Field.FEE_PCT),
            kazpostDays = this[Field.KAZPOST_DAYS].trim().ifEmpty { CalculatorInput.DEFAULT_KAZPOST_DAYS },
            cargoRateUsdPerKg = number(Field.CARGO_RATE),
            cargoDays = this[Field.CARGO_DAYS].trim().ifEmpty { CalculatorInput.DEFAULT_CARGO_DAYS },
        ),
        errors = errors,
        qtyHint = if (qtyRaw.isBlank()) "Пусто — считаем 1 шт" else null,
    )
}

data class CalculatorUiState(
    val fields: CalculatorFields,
    val parsed: ParsedInput,
    val rates: RatesState,
    val result: CalculationResult,
) {
    val refreshing: Boolean get() = rates.status == RatesStatus.Loading
}

class PddCalculatorViewModel(private val ratesRepository: RatesRepository) : ViewModel() {

    private val calculate = CalculateDeliveryUseCase()
    private val fields = MutableStateFlow(CalculatorFields())
    private var refreshJob: Job? = null

    private fun state(fields: CalculatorFields, rates: RatesState): CalculatorUiState {
        val parsed = fields.parse()
        return CalculatorUiState(fields, parsed, rates, calculate(parsed.input, rates.rates))
    }

    /** Пересчёт мгновенный, без дебаунса: математика дешёвая. */
    val uiState: StateFlow<CalculatorUiState> = combine(fields, ratesRepository.state, ::state)
        .stateIn(viewModelScope, SharingStarted.Eagerly, state(fields.value, ratesRepository.state.value))

    init {
        refreshRates()
    }

    fun onFieldChange(field: Field, text: String) {
        val clean = when (field) {
            Field.KAZPOST_DAYS, Field.CARGO_DAYS -> text.take(12)
            Field.ITEM_QTY -> text.filter(Char::isDigit).take(6)
            else -> text.filter { it.isDigit() || it == '.' || it == ',' }.take(12)
        }
        fields.update { it.copy(text = it.text + (field to clean)) }
    }

    fun onCurrencyChange(currency: DisplayCurrency) = fields.update { it.copy(currency = currency) }

    /** Курсы живут в репозитории, поэтому сброс их не трогает. */
    fun resetDefaults() {
        fields.value = CalculatorFields()
    }

    fun refreshRates() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch { ratesRepository.refresh() }
    }

    class Factory(private val ratesRepository: RatesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PddCalculatorViewModel(ratesRepository) as T
    }
}
