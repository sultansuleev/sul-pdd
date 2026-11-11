package com.suldev.pddkz.domain.model

enum class DisplayCurrency { KZT, USD }

/** Дефолты §2.1; `CalculatorInput()` — это и есть `resetDefaults`. */
data class CalculatorInput(
    val currency: DisplayCurrency = DisplayCurrency.KZT,
    val itemYuan: Double = 180.0,
    val itemWeightKg: Double = 1.2,
    val itemQty: Int = 1,
    val kazpostVatPct: Double = 16.0,
    val kazpostFeePct: Double = 3.0,
    val kazpostDays: String = DEFAULT_KAZPOST_DAYS,
    val cargoRateUsdPerKg: Double = 3.5,
    val cargoDays: String = DEFAULT_CARGO_DAYS,
) {
    companion object {
        const val DEFAULT_KAZPOST_DAYS = "10–18"
        const val DEFAULT_CARGO_DAYS = "12–25"
    }
}
