package com.suldev.pddkz.domain.usecase

import com.suldev.pddkz.domain.model.CalculationResult
import com.suldev.pddkz.domain.model.CalculatorInput
import com.suldev.pddkz.domain.model.ComparisonOutcome
import com.suldev.pddkz.domain.model.NbkRates

/** Чистая математика §2.3 и §2.5. */
class CalculateDeliveryUseCase {

    operator fun invoke(input: CalculatorInput, rates: NbkRates): CalculationResult {
        // Мусор на входе (минус, NaN, бесконечность) считаем нулём, количество — не меньше 1.
        val qty = input.itemQty.coerceAtLeast(1)
        val itemYuan = input.itemYuan.safe()
        val vatPct = input.kazpostVatPct.safe()
        val feePct = input.kazpostFeePct.safe()
        val rateCny = rates.cnyKzt.safe()
        val rateUsd = rates.usdKzt.safe()

        val totalYuan = itemYuan * qty
        val totalWeightKg = input.itemWeightKg.safe() * qty
        val baseKzt = totalYuan * rateCny

        val vatKzt = baseKzt * vatPct / 100
        val feeKzt = baseKzt * feePct / 100
        val kazpostTotalKzt = baseKzt + vatKzt + feeKzt
        val ratio = if (baseKzt > 0) kazpostTotalKzt / baseKzt else 1.0

        val cargoShippingUsd = totalWeightKg * input.cargoRateUsdPerKg.safe()
        val cargoShippingKzt = cargoShippingUsd * rateUsd
        val cargoTotalKzt = baseKzt + cargoShippingKzt

        val outcome = when {
            totalYuan <= 0.0 -> ComparisonOutcome.NoPrice
            kazpostTotalKzt < cargoTotalKzt -> (cargoTotalKzt - kazpostTotalKzt).let {
                ComparisonOutcome.KazpostWins(it, it / cargoTotalKzt * 100)
            }
            cargoTotalKzt < kazpostTotalKzt -> (kazpostTotalKzt - cargoTotalKzt).let {
                ComparisonOutcome.CargoWins(it, it / kazpostTotalKzt * 100)
            }
            else -> ComparisonOutcome.Equal
        }

        val breakEvenDenominator = (vatPct + feePct) * rateCny * qty

        return CalculationResult(
            totalYuan = totalYuan,
            totalWeightKg = totalWeightKg,
            baseKzt = baseKzt,
            kazpostVatAmtKzt = vatKzt,
            kazpostFeeAmtKzt = feeKzt,
            kazpostTotalKzt = kazpostTotalKzt,
            kazpostTotalYuan = totalYuan * ratio,
            cargoShippingUsd = cargoShippingUsd,
            cargoShippingKzt = cargoShippingKzt,
            cargoTotalKzt = cargoTotalKzt,
            cargoTotalYuan = totalYuan, // базовая модель, паритет с веб-эталоном
            outcome = outcome,
            breakEvenItemYuan = if (breakEvenDenominator > 0) cargoShippingKzt * 100 / breakEvenDenominator else null,
        )
    }

    private fun Double.safe() = if (isFinite() && this > 0) this else 0.0
}
