package com.suldev.pddkz.domain

import com.suldev.pddkz.domain.model.DisplayCurrency
import java.util.Locale

// Форматирование §2.4 и парсинг локального ввода §2.3.

/** Округление до целого с группировкой тысяч неразрывным пробелом: `14 283`. */
fun groupedRound(value: Double): String =
    // Разделитель задан явно: у Locale("ru") он разный на разных версиях Android (U+00A0 / U+202F).
    String.format(Locale.US, "%,d", Math.round(value)).replace(',', '\u00A0')

fun formatCurrency(kztValue: Double, currency: DisplayCurrency, rateUsdKzt: Double): String = when (currency) {
    DisplayCurrency.KZT -> "${groupedRound(kztValue)} ₸"
    DisplayCurrency.USD -> String.format(Locale.US, "$ %.2f", if (rateUsdKzt > 0) kztValue / rateUsdKzt else 0.0)
}

fun formatYuan(yuanValue: Double): String = "≈ ${groupedRound(yuanValue)} ¥"

fun formatPct(pct: Double): String = String.format(Locale.US, "%.1f", pct)

/** Принимает `,` и `.`; `null` для пустого, нечислового, отрицательного и бесконечного ввода. */
fun parseDecimal(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }

/** Количество: целое `>= 1`, иначе `null` (вызывающий подставляет фолбэк `1` и показывает подсказку). */
fun parseQty(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it >= 1 }

/** Курс НБ РК для поля: два знака, десятичная запятая — `66,68`. */
fun formatRate(value: Double): String = String.format(Locale.US, "%.2f", value).replace('.', ',')

/** Сумма в долларах с двумя знаками: `$ 4.20`. */
fun formatUsd(usdValue: Double): String = String.format(Locale.US, "$ %.2f", usdValue)

/** Компактное число для формул: до 2 знаков после точки, без хвостовых нулей и с группировкой тысяч. */
fun formatFormulaDecimal(value: Double): String {
    if (!value.isFinite()) return "0"
    val rounded = kotlin.math.round(value * 100.0) / 100.0
    if (rounded % 1.0 == 0.0) return groupedRound(rounded)
    val whole = rounded.toLong()
    val frac = String.format(Locale.US, "%.2f", kotlin.math.abs(rounded - whole)).substring(1).trimEnd('0')
    val groupedWhole = if (rounded < 0 && whole == 0L) "-0" else groupedRound(whole.toDouble())
    return groupedWhole + frac
}
