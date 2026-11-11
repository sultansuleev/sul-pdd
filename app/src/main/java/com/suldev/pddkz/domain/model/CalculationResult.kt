package com.suldev.pddkz.domain.model

data class CalculationResult(
    val totalYuan: Double,
    val totalWeightKg: Double,
    val baseKzt: Double,
    val kazpostVatAmtKzt: Double,
    val kazpostFeeAmtKzt: Double,
    val kazpostTotalKzt: Double,
    val kazpostTotalYuan: Double,
    val cargoShippingUsd: Double,
    val cargoShippingKzt: Double,
    val cargoTotalKzt: Double,
    val cargoTotalYuan: Double,
    val outcome: ComparisonOutcome,
    /** Цена 1 шт в ¥, при которой варианты равны; `null`, если НДС + комиссия = 0 или курс ¥ = 0. */
    val breakEvenItemYuan: Double?,
)

/** Четыре исхода §2.5. Тексты — забота UI. */
sealed interface ComparisonOutcome {
    data object NoPrice : ComparisonOutcome
    data object Equal : ComparisonOutcome
    data class KazpostWins(val diffKzt: Double, val pct: Double) : ComparisonOutcome
    data class CargoWins(val diffKzt: Double, val pct: Double) : ComparisonOutcome
}
