package com.suldev.pddkz

import com.suldev.pddkz.domain.formatCurrency
import com.suldev.pddkz.domain.formatFormulaDecimal
import com.suldev.pddkz.domain.formatPct
import com.suldev.pddkz.domain.formatUsd
import com.suldev.pddkz.domain.formatYuan
import com.suldev.pddkz.domain.model.CalculatorInput
import com.suldev.pddkz.domain.model.ComparisonOutcome
import com.suldev.pddkz.domain.model.DisplayCurrency
import com.suldev.pddkz.domain.model.NbkRates
import com.suldev.pddkz.domain.parseDecimal
import com.suldev.pddkz.domain.parseQty
import com.suldev.pddkz.domain.usecase.CalculateDeliveryUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateDeliveryUseCaseTest {

    private val calculate = CalculateDeliveryUseCase()
    private val rates = NbkRates() // 68.11 / 453.71
    private val eps = 1e-6

    @Test
    fun `defaults - cargo wins`() {
        val r = calculate(CalculatorInput(), rates)

        assertEquals(180.0, r.totalYuan, eps)
        assertEquals(1.2, r.totalWeightKg, eps)
        assertEquals(12259.8, r.baseKzt, eps)
        assertEquals(1961.568, r.kazpostVatAmtKzt, eps)
        assertEquals(367.794, r.kazpostFeeAmtKzt, eps)
        assertEquals(14589.162, r.kazpostTotalKzt, eps)
        assertEquals(214.2, r.kazpostTotalYuan, eps)
        assertEquals(4.2, r.cargoShippingUsd, eps)
        assertEquals(1905.582, r.cargoShippingKzt, eps)
        assertEquals(14165.382, r.cargoTotalKzt, eps)
        assertEquals(180.0, r.cargoTotalYuan, eps)

        val outcome = r.outcome as ComparisonOutcome.CargoWins
        assertEquals(423.78, outcome.diffKzt, eps)
        assertEquals("2.9", formatPct(outcome.pct))
    }

    @Test
    fun `light cheap item - kazpost wins`() {
        val r = calculate(CalculatorInput(itemYuan = 50.0, itemWeightKg = 3.0), rates)

        val outcome = r.outcome as ComparisonOutcome.KazpostWins
        assertEquals(8169.455 - 4052.545, outcome.diffKzt, eps)
        assertEquals("50.4", formatPct(outcome.pct))
    }

    @Test
    fun `zero price - neutral`() {
        val r = calculate(CalculatorInput(itemYuan = 0.0), rates)

        assertEquals(ComparisonOutcome.NoPrice, r.outcome)
        assertEquals(0.0, r.kazpostTotalKzt, eps)
        assertEquals(0.0, r.kazpostTotalYuan, eps) // ratio = 1 при baseKzt = 0, без деления на ноль
    }

    @Test
    fun `equal totals`() {
        val r = calculate(CalculatorInput(kazpostVatPct = 0.0, kazpostFeePct = 0.0, itemWeightKg = 0.0), rates)

        assertEquals(ComparisonOutcome.Equal, r.outcome)
        assertNull(r.breakEvenItemYuan)
    }

    @Test
    fun `quantity multiplies price and weight`() {
        val one = calculate(CalculatorInput(), rates)
        val three = calculate(CalculatorInput(itemQty = 3), rates)

        assertEquals(one.kazpostTotalKzt * 3, three.kazpostTotalKzt, eps)
        assertEquals(one.cargoTotalKzt * 3, three.cargoTotalKzt, eps)
        assertEquals(one.breakEvenItemYuan!!, three.breakEvenItemYuan!!, eps)
    }

    @Test
    fun `break-even price makes totals equal`() {
        val breakEven = calculate(CalculatorInput(), rates).breakEvenItemYuan!!
        assertEquals(1905.582 * 100 / (19 * 68.11), breakEven, eps)

        val r = calculate(CalculatorInput(itemYuan = breakEven), rates)
        assertEquals(r.kazpostTotalKzt, r.cargoTotalKzt, eps)
    }

    @Test
    fun `garbage input does not produce NaN`() {
        val r = calculate(
            CalculatorInput(itemYuan = Double.NaN, itemWeightKg = -5.0, itemQty = 0, kazpostVatPct = Double.POSITIVE_INFINITY),
            NbkRates(cnyKzt = 0.0, usdKzt = 0.0),
        )

        assertEquals(ComparisonOutcome.NoPrice, r.outcome)
        assertTrue(listOf(r.kazpostTotalKzt, r.cargoTotalKzt, r.kazpostTotalYuan).all { it == 0.0 })
        assertNull(r.breakEvenItemYuan)
    }

    @Test
    fun `currency formatting KZT and USD`() {
        assertEquals("14\u00A0165 ₸", formatCurrency(14165.382, DisplayCurrency.KZT, 453.71))
        assertEquals("$ 31.22", formatCurrency(14165.382, DisplayCurrency.USD, 453.71))
        assertEquals("0 ₸", formatCurrency(0.0, DisplayCurrency.KZT, 453.71))
        assertEquals("$ 0.00", formatCurrency(14165.382, DisplayCurrency.USD, 0.0))
        assertEquals("≈ 214 ¥", formatYuan(214.2))
        assertEquals("≈ 1\u00A0234\u00A0568 ¥", formatYuan(1234567.5))
        assertEquals("$ 4.20", formatUsd(4.2))
        assertEquals("147.25", formatFormulaDecimal(147.2528))
        assertEquals("180", formatFormulaDecimal(180.0))
        assertEquals("1\u00A0234.5", formatFormulaDecimal(1234.5))
    }

    @Test
    fun `break-even is zero when cargo tariff or weight is zero`() {
        val r = calculate(CalculatorInput(cargoRateUsdPerKg = 0.0), rates)
        assertEquals(0.0, r.breakEvenItemYuan!!, eps)
    }

    @Test
    fun `input parsing`() {
        assertEquals(1.5, parseDecimal("1,5")!!, eps)
        assertEquals(1.5, parseDecimal(" 1.5 ")!!, eps)
        assertNull(parseDecimal(""))
        assertNull(parseDecimal("abc"))
        assertNull(parseDecimal("-1"))
        assertNull(parseDecimal("NaN"))
        assertNull(parseDecimal("1,2,3"))

        assertEquals(2, parseQty("2"))
        assertNull(parseQty(""))
        assertNull(parseQty("0"))
        assertNull(parseQty("1.5"))
    }
}
