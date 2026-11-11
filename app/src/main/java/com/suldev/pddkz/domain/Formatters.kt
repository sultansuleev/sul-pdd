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
