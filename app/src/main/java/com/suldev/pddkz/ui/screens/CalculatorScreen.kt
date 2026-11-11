package com.suldev.pddkz.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.suldev.kit.touch.IosOverscrollEffect
import com.suldev.pddkz.domain.formatCurrency
import com.suldev.pddkz.domain.formatRate
import com.suldev.pddkz.domain.formatYuan
import com.suldev.pddkz.domain.model.ComparisonOutcome
import com.suldev.pddkz.domain.model.DisplayCurrency
import com.suldev.pddkz.ui.components.CargoIcon
import com.suldev.pddkz.ui.components.CostRow
import com.suldev.pddkz.ui.components.DeliveryMethodCard
import com.suldev.pddkz.ui.components.GlassCard
import com.suldev.pddkz.ui.components.HintSection
import com.suldev.pddkz.ui.components.IosIconButton
import com.suldev.pddkz.ui.components.IosNumberField
import com.suldev.pddkz.ui.components.KazpostIcon
import com.suldev.pddkz.ui.components.ScreenList
import com.suldev.pddkz.ui.components.SectionHeader
import com.suldev.pddkz.ui.components.SegmentedControl
import com.suldev.pddkz.ui.components.TintedBlock
import com.suldev.pddkz.ui.components.WinnerBanner
import com.suldev.pddkz.ui.theme.PddTheme
import com.suldev.pddkz.ui.viewmodel.CalculatorUiState
import com.suldev.pddkz.ui.viewmodel.Field
import com.suldev.pddkz.ui.viewmodel.toFieldText

@Composable
fun CalculatorScreen(
    state: CalculatorUiState,
    listState: LazyListState,
    overscroll: IosOverscrollEffect,
    onFieldChange: (Field, String) -> Unit,
    onCurrencyChange: (DisplayCurrency) -> Unit,
    onRefreshRates: () -> Unit,
) {
    val c = PddTheme.colors
    val input = state.parsed.input
    val result = state.result
    val rates = state.rates.rates
    fun money(kzt: Double) = formatCurrency(kzt, input.currency, rates.usdKzt)

    // Поле ввода, привязанное к состоянию: текст, ошибка и обработчик берутся по ключу.
    @Composable
    fun Input(
        field: Field,
        label: String,
        modifier: Modifier = Modifier,
        prefix: String? = null,
        suffix: String? = null,
        keyboardType: KeyboardType = KeyboardType.Decimal,
        hint: String? = null,
    ) = IosNumberField(
        label = label,
        value = state.fields[field],
        onValueChange = { onFieldChange(field, it) },
        modifier = modifier,
        prefix = prefix,
        suffix = suffix,
        keyboardType = keyboardType,
        error = state.parsed.errors[field],
        hint = hint,
    )

    ScreenList(
        title = "Pinduoduo KZ Calc",
        subtitle = "Сравнение доставки в Казахстан",
        listState = listState,
        overscroll = overscroll,
    ) {
        // 1. Валюта отображения
        item(key = "currency") {
            SegmentedControl(
                options = listOf("₸ KZT", "$ USD"),
                selectedIndex = input.currency.ordinal,
                onSelect = { onCurrencyChange(DisplayCurrency.entries[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // 2. Результат сравнения
        item(key = "banner") { WinnerBanner(result.outcome, input.currency, rates.usdKzt) }

        // 3. Карточки вариантов: колонкой на телефоне, в два столбца от 600 dp
        item(key = "methods") {
            val kazpost: @Composable (Modifier) -> Unit = { m ->
                DeliveryMethodCard(
                    title = "Казпочта",
                    tag = "Прямая",
                    icon = KazpostIcon,
                    accent = c.kazpost,
                    container = c.kazpostContainer,
                    daysText = "${input.kazpostDays} дней",
                    total = money(result.kazpostTotalKzt),
                    totalYuan = formatYuan(result.kazpostTotalYuan),
                    rows = listOf(
                        CostRow("Товар (перевод):", money(result.baseKzt)),
                        CostRow("НДС (${input.kazpostVatPct.toFieldText()}%):", money(result.kazpostVatAmtKzt)),
                        CostRow("Комиссия (${input.kazpostFeePct.toFieldText()}%):", money(result.kazpostFeeAmtKzt)),
                        CostRow("Доставка в отделение:", "0 (Бесплатно)", positive = true),
                    ),
                    winner = result.outcome is ComparisonOutcome.KazpostWins,
                    dimmed = result.outcome is ComparisonOutcome.CargoWins,
                    modifier = m,
                )
            }
            val cargo: @Composable (Modifier) -> Unit = { m ->
                DeliveryMethodCard(
                    title = "Карго Компания",
                    tag = "Посредник",
                    icon = CargoIcon,
                    accent = c.cargo,
                    container = c.cargoContainer,
                    daysText = "${input.cargoDays} дней",
                    total = money(result.cargoTotalKzt),
                    totalYuan = formatYuan(result.cargoTotalYuan),
                    rows = listOf(
                        CostRow("Товар (перевод):", money(result.baseKzt)),
                        CostRow("Доставка за вес:", money(result.cargoShippingKzt)),
                        CostRow("Налоги / Таможня:", "0 (Включено)", positive = true),
                    ),
                    winner = result.outcome is ComparisonOutcome.CargoWins,
                    dimmed = result.outcome is ComparisonOutcome.KazpostWins,
                    modifier = m,
                )
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 600.dp) {
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        kazpost(Modifier.weight(1f).fillMaxHeight())
                        cargo(Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        kazpost(Modifier.fillMaxWidth())
                        cargo(Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // 4. Параметры посылки
        item(key = "parcel") {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionHeader("Параметры посылки") {
                    Text("PINDUODUO / 拼多多", style = MaterialTheme.typography.labelSmall, color = c.secondaryLabel)
                }
                Input(
                    Field.ITEM_YUAN,
                    "Стоимость товара в Пиндодо (¥ Yuan)",
                    Modifier.fillMaxWidth(),
                    prefix = "¥",
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Input(Field.ITEM_WEIGHT, "Вес 1 шт (кг)", Modifier.weight(1f), suffix = "кг")
                    Input(
                        Field.ITEM_QTY,
                        "Количество (шт)",
                        Modifier.weight(1f),
                        suffix = "шт",
                        keyboardType = KeyboardType.Number,
                        hint = state.parsed.qtyHint,
                    )
                }
            }
        }

        // 5. Курсы НБ РК
        item(key = "rates") {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionHeader("Курсы Нац. Банка РК") {
                    Text(
                        state.rates.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = c.secondaryLabel,
                    )
                    IosIconButton(
                        icon = Icons.Filled.Refresh,
                        contentDescription = "Обновить курсы НБ РК",
                        onClick = onRefreshRates,
                        enabled = !state.refreshing,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IosNumberField(
                        label = "Курс 1 ¥ (Юань)",
                        value = formatRate(rates.cnyKzt),
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        suffix = "₸",
                        readOnly = true,
                    )
                    IosNumberField(
                        label = "Курс 1 $ (Доллар)",
                        value = formatRate(rates.usdKzt),
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        suffix = "₸",
                        readOnly = true,
                    )
                }
            }
        }

        // 6. Тарифы и сроки
        item(key = "tariffs") {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionHeader("Настройка тарифов и сроков")
                TintedBlock(c.kazpostContainer, Modifier.fillMaxWidth()) {
                    BoxWithConstraints {
                        val narrow = maxWidth < 280.dp // экран уже 360 dp: три поля в ряд не помещаются
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Казпочта (Прямая) · Официальная",
                                style = MaterialTheme.typography.titleSmall,
                                color = c.kazpost,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Input(Field.VAT_PCT, "НДС (%)", Modifier.weight(1f))
                                Input(Field.FEE_PCT, "Комиссия (%)", Modifier.weight(1f))
                                if (!narrow) {
                                    Input(
                                        Field.KAZPOST_DAYS, "Сроки (дней)", Modifier.weight(1f),
                                        keyboardType = KeyboardType.Text,
                                    )
                                }
                            }
                            if (narrow) {
                                Input(
                                    Field.KAZPOST_DAYS, "Сроки (дней)", Modifier.fillMaxWidth(),
                                    keyboardType = KeyboardType.Text,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TintedBlock(c.cargoContainer, Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Карго Доставка · Посредник",
                            style = MaterialTheme.typography.titleSmall,
                            color = c.cargo,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Input(Field.CARGO_RATE, "Тариф ($/кг)", Modifier.weight(1f))
                            Input(
                                Field.CARGO_DAYS, "Сроки (дней)", Modifier.weight(1f),
                                keyboardType = KeyboardType.Text,
                            )
                        }
                    }
                }
            }
        }

        // 7. Подсказки
        item(key = "hints") { HintSection() }
    }
}
