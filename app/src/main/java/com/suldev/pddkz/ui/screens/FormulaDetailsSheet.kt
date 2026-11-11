package com.suldev.pddkz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suldev.pddkz.domain.formatCurrency
import com.suldev.pddkz.domain.formatFormulaDecimal
import com.suldev.pddkz.domain.formatRate
import com.suldev.pddkz.domain.formatUsd
import com.suldev.pddkz.domain.formatYuan
import com.suldev.pddkz.domain.groupedRound
import com.suldev.pddkz.domain.model.ComparisonOutcome
import com.suldev.pddkz.domain.model.DisplayCurrency
import com.suldev.pddkz.ui.components.Capsule
import com.suldev.pddkz.ui.components.CargoIcon
import com.suldev.pddkz.ui.components.IosButton
import com.suldev.pddkz.ui.components.KazpostIcon
import com.suldev.pddkz.ui.components.TintedBlock
import com.suldev.pddkz.ui.theme.PddTheme
import com.suldev.pddkz.ui.viewmodel.CalculatorUiState
import com.suldev.pddkz.ui.viewmodel.toFieldText

enum class FormulaFocus { ALL, KAZPOST, CARGO }

/**
 * Шторка «Детали расчёта и формулы» (§5.3): пошаговая математика на текущих числах,
 * точка безубыточности и кнопка «Закрыть».
 */
@Composable
fun ColumnScope.FormulaDetailsSheet(
    state: CalculatorUiState,
    focus: FormulaFocus,
    scrollModifier: Modifier,
    onDismiss: () -> Unit,
) {
    val c = PddTheme.colors
    val input = state.parsed.input
    val result = state.result
    val rates = state.rates.rates
    val kzt = { value: Double -> "${groupedRound(value)} ₸" }
    val qty = input.itemQty.coerceAtLeast(1)

    // Шапка шторки: закреплена над прокручиваемым списком, тянет шторку свайпом вниз.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Детали расчёта и формулы",
            style = MaterialTheme.typography.titleLarge,
            color = c.label,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "${formatFormulaDecimal(input.itemYuan)} ¥ · ${formatFormulaDecimal(input.itemWeightKg)} кг · " +
                "$qty шт · НБ РК: 1 ¥ = ${formatRate(rates.cnyKzt)} ₸, 1 $ = ${formatRate(rates.usdKzt)} ₸",
            style = MaterialTheme.typography.labelMedium,
            color = c.secondaryLabel,
        )
    }

    Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.separator))

    val baseBlock: @Composable () -> Unit = {
        FormulaBlock(
            title = "Базовые величины",
            icon = Icons.Filled.Calculate,
            accent = c.tint,
            container = c.separator.copy(alpha = 0.14f),
            highlighted = false,
            badge = null,
        ) {
            FormulaStep(
                label = "Общая стоимость в юанях",
                formula = "${input.itemYuan.toFieldText()} ¥ × $qty шт",
                result = "${formatFormulaDecimal(result.totalYuan)} ¥",
            )
            FormulaStep(
                label = "Общий вес посылки",
                formula = "${input.itemWeightKg.toFieldText()} кг × $qty шт",
                result = "${formatFormulaDecimal(result.totalWeightKg)} кг",
            )
            FormulaStep(
                label = "База товара в тенге",
                formula = "${formatFormulaDecimal(result.totalYuan)} ¥ × ${formatRate(rates.cnyKzt)} ₸",
                result = kzt(result.baseKzt),
            )
            if (input.currency == DisplayCurrency.USD) {
                FormulaStep(
                    label = "Перевод базы в USD",
                    formula = "${kzt(result.baseKzt)} / ${formatRate(rates.usdKzt)} ₸",
                    result = formatCurrency(result.baseKzt, DisplayCurrency.USD, rates.usdKzt),
                )
            }
        }
    }

    val kazpostBlock: @Composable () -> Unit = {
        val ratioText = if (result.baseKzt > 0) {
            "${groupedRound(result.kazpostTotalKzt)} / ${groupedRound(result.baseKzt)}"
        } else {
            "1"
        }
        val kazpostBadge = when {
            result.outcome is ComparisonOutcome.KazpostWins -> "Выгоднее"
            focus == FormulaFocus.KAZPOST -> "Выбрано"
            else -> null
        }
        FormulaBlock(
            title = "Казпочта (Прямая)",
            icon = KazpostIcon,
            accent = c.kazpost,
            container = c.kazpostContainer,
            highlighted = focus == FormulaFocus.KAZPOST,
            badge = kazpostBadge,
        ) {
            FormulaStep(
                label = "НДС (${input.kazpostVatPct.toFieldText()}%)",
                formula = "${kzt(result.baseKzt)} × ${input.kazpostVatPct.toFieldText()} / 100",
                result = kzt(result.kazpostVatAmtKzt),
            )
            FormulaStep(
                label = "Комиссия (${input.kazpostFeePct.toFieldText()}%)",
                formula = "${kzt(result.baseKzt)} × ${input.kazpostFeePct.toFieldText()} / 100",
                result = kzt(result.kazpostFeeAmtKzt),
            )
            FormulaStep(
                label = "Доставка в отделение",
                formula = "Тариф отделения (срок ${input.kazpostDays} дн.)",
                result = "0 (Бесплатно)",
                resultColor = c.positive,
            )
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.separator))
            FormulaStep(
                label = "Итого Казпочта",
                formula = "${groupedRound(result.baseKzt)} + ${groupedRound(result.kazpostVatAmtKzt)} + ${groupedRound(result.kazpostFeeAmtKzt)} ₸",
                result = if (input.currency == DisplayCurrency.USD) {
                    "${kzt(result.kazpostTotalKzt)} (${formatCurrency(result.kazpostTotalKzt, DisplayCurrency.USD, rates.usdKzt)})"
                } else {
                    kzt(result.kazpostTotalKzt)
                },
                resultColor = c.kazpost,
                bold = true,
            )
            FormulaStep(
                label = "Эквивалент в юанях",
                formula = "${formatFormulaDecimal(result.totalYuan)} ¥ × ($ratioText)",
                result = formatYuan(result.kazpostTotalYuan),
            )
        }
    }

    val cargoBlock: @Composable () -> Unit = {
        val cargoBadge = when {
            result.outcome is ComparisonOutcome.CargoWins -> "Выгоднее"
            focus == FormulaFocus.CARGO -> "Выбрано"
            else -> null
        }
        FormulaBlock(
            title = "Карго Компания (Посредник)",
            icon = CargoIcon,
            accent = c.cargo,
            container = c.cargoContainer,
            highlighted = focus == FormulaFocus.CARGO,
            badge = cargoBadge,
        ) {
            FormulaStep(
                label = "Доставка за вес (USD)",
                formula = "${formatFormulaDecimal(result.totalWeightKg)} кг × ${input.cargoRateUsdPerKg.toFieldText()} $/кг",
                result = formatUsd(result.cargoShippingUsd),
            )
            FormulaStep(
                label = "Доставка за вес (KZT)",
                formula = "${formatUsd(result.cargoShippingUsd)} × ${formatRate(rates.usdKzt)} ₸",
                result = kzt(result.cargoShippingKzt),
            )
            FormulaStep(
                label = "Налоги и таможня",
                formula = "Включены в тариф (срок ${input.cargoDays} дн.)",
                result = "0 (Включено)",
                resultColor = c.positive,
            )
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.separator))
            FormulaStep(
                label = "Итого Карго",
                formula = "${groupedRound(result.baseKzt)} + ${groupedRound(result.cargoShippingKzt)} ₸",
                result = if (input.currency == DisplayCurrency.USD) {
                    "${kzt(result.cargoTotalKzt)} (${formatCurrency(result.cargoTotalKzt, DisplayCurrency.USD, rates.usdKzt)})"
                } else {
                    kzt(result.cargoTotalKzt)
                },
                resultColor = c.cargo,
                bold = true,
            )
            FormulaStep(
                label = "Эквивалент в юанях",
                formula = "Базовая модель товара",
                result = formatYuan(result.cargoTotalYuan),
            )
        }
    }

    val breakEvenBlock: @Composable () -> Unit = {
        val vatFeeSum = input.kazpostVatPct.coerceAtLeast(0.0) + input.kazpostFeePct.coerceAtLeast(0.0)
        val breakEven = result.breakEvenItemYuan
        FormulaBlock(
            title = "Точка безубыточности",
            icon = Icons.Filled.Balance,
            accent = c.tint,
            container = c.separator.copy(alpha = 0.14f),
            highlighted = false,
            badge = breakEven?.let { "${formatFormulaDecimal(it)} ¥" },
        ) {
            Text(
                text = "Цена 1 шт, при которой Казпочта и Карго выходят в одну сумму при текущем весе и тарифах:",
                style = MaterialTheme.typography.bodySmall,
                color = c.secondaryLabel,
            )
            if (breakEven != null) {
                FormulaStep(
                    label = "Расчёт порога (1 шт)",
                    formula = "(${kzt(result.cargoShippingKzt)} × 100) / (${formatFormulaDecimal(vatFeeSum)}% × ${formatRate(rates.cnyKzt)} ₸ × $qty)",
                    result = "${formatFormulaDecimal(breakEven)} ¥",
                    resultColor = c.tint,
                    bold = true,
                )
                val takeaway = if (breakEven > 0.0) {
                    "До ${formatFormulaDecimal(breakEven)} ¥ за 1 шт выгоднее Казпочта; выше этого порога дешевле Карго."
                } else {
                    "Стоимость доставки Карго сейчас равна 0 ₸ — при любой положительной цене товара Карго не дороже Казпочты."
                }
                Text(
                    text = takeaway,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.label,
                )
            } else {
                Text(
                    text = "Не определена: сумма НДС и комиссии Казпочты равна 0% либо курс юаня равен 0.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.label,
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .weight(1f, fill = false)
            .fillMaxWidth()
            .then(scrollModifier),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "base") { baseBlock() }
        if (focus == FormulaFocus.CARGO) {
            item(key = "cargo") { cargoBlock() }
            item(key = "kazpost") { kazpostBlock() }
        } else {
            item(key = "kazpost") { kazpostBlock() }
            item(key = "cargo") { cargoBlock() }
        }
        item(key = "breakeven") { breakEvenBlock() }
    }

    Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.separator))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        IosButton(
            label = "Закрыть",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FormulaBlock(
    title: String,
    icon: ImageVector,
    accent: Color,
    container: Color,
    highlighted: Boolean,
    badge: String?,
    content: @Composable () -> Unit,
) {
    val c = PddTheme.colors
    TintedBlock(
        container = container,
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (highlighted) accent else c.separator,
        borderWidth = if (highlighted) 1.dp else 0.5.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = c.label,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (badge != null) {
                    Capsule(text = badge, color = accent, container = c.surface)
                }
            }
            content()
        }
    }
}

@Composable
private fun FormulaStep(
    label: String,
    formula: String,
    result: String,
    resultColor: Color = PddTheme.colors.label,
    bold: Boolean = false,
) {
    val c = PddTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = c.label)
            Text(text = formula, style = MaterialTheme.typography.labelMedium, color = c.secondaryLabel)
        }
        Text(
            text = result,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            color = resultColor,
        )
    }
}
