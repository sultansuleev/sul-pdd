package com.suldev.pddkz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialfluid.compose.MaterialFluidCapabilities
import com.materialfluid.compose.MaterialFluidMaterial
import com.suldev.kit.haptics.HapticPattern
import com.suldev.kit.haptics.rememberHapticDevice
import com.suldev.pddkz.domain.formatRate
import com.suldev.pddkz.domain.model.AppSettings
import com.suldev.pddkz.domain.model.BackgroundStyle
import com.suldev.pddkz.domain.model.RatesState
import com.suldev.pddkz.domain.model.RatesStatus
import com.suldev.pddkz.domain.model.ThemeMode
import com.suldev.pddkz.ui.components.GlassCard
import com.suldev.pddkz.ui.components.IosButton
import com.suldev.pddkz.ui.components.ScreenList
import com.suldev.pddkz.ui.components.SectionHeader
import com.suldev.pddkz.ui.components.SegmentedControl
import com.suldev.pddkz.ui.components.playToggleHaptic
import com.suldev.pddkz.ui.theme.PddTheme
import kotlin.math.roundToInt

private val Themes = listOf(ThemeMode.SYSTEM to "Система", ThemeMode.LIGHT to "Светлая", ThemeMode.DARK to "Тёмная")
private val Materials = listOf(MaterialFluidMaterial.Liquid to "Жидкое", MaterialFluidMaterial.Frosted to "Матовое")
private val Backgrounds = listOf(
    BackgroundStyle.PLAIN to "Однотон",
    BackgroundStyle.MESH to "Меш",
    BackgroundStyle.IMAGE to "Фото",
)

@Composable
fun SettingsScreen(
    listState: LazyListState,
    settings: AppSettings,
    hasBackgroundPhoto: Boolean,
    rates: RatesState,
    highContrast: Boolean,
    reduceMotion: Boolean,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onPreviewDimOpacity: (Float?) -> Unit,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    onRefreshRates: () -> Unit,
) {
    val c = PddTheme.colors
    val view = LocalView.current
    val haptics = rememberHapticDevice()

    ScreenList(title = "Настройки", listState = listState) {
        // 1. Внешний вид
        item(key = "appearance") {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("Внешний вид")
                    Option("Тема оформления", Themes, settings.themeMode) { v ->
                        onUpdateSettings { it.copy(themeMode = v) }
                    }
                    Option("Материал стекла", Materials, settings.material) { v ->
                        onUpdateSettings { it.copy(material = v) }
                    }
                    if (!MaterialFluidCapabilities.isShaderSupported) {
                        Text(
                            text = "Шейдерное стекло доступно на Android 13+, на этом устройстве используется упрощённая заливка",
                            style = MaterialTheme.typography.labelMedium,
                            color = c.secondaryLabel,
                        )
                    }
                    Option("Фон экрана", Backgrounds, settings.backgroundStyle) { v ->
                        onUpdateSettings { it.copy(backgroundStyle = v) }
                    }

                    if (settings.backgroundStyle == BackgroundStyle.IMAGE) {
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.separator))
                        if (!hasBackgroundPhoto) {
                            Text(
                                text = "Выберите изображение из галереи для фона экрана",
                                style = MaterialTheme.typography.labelMedium,
                                color = c.secondaryLabel,
                            )
                        }
                        PhotoActionsRow(
                            hasPhoto = hasBackgroundPhoto,
                            onPickPhoto = onPickPhoto,
                            onClearPhoto = onClearPhoto,
                        )

                        var sliderDim by remember(settings.dimOpacity) { mutableFloatStateOf(settings.dimOpacity) }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Затемнение фото", style = MaterialTheme.typography.bodyMedium, color = c.label)
                                Text(
                                    text = "${(sliderDim * 100f).roundToInt()}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = c.label,
                                )
                            }
                            Slider(
                                value = sliderDim,
                                onValueChange = { value ->
                                    val clamped = value.coerceIn(AppSettings.DIM_MIN, AppSettings.DIM_MAX)
                                    sliderDim = clamped
                                    onPreviewDimOpacity(clamped)
                                },
                                onValueChangeFinished = {
                                    haptics.play(HapticPattern.Tick)
                                    val finalDim = sliderDim.coerceIn(AppSettings.DIM_MIN, AppSettings.DIM_MAX)
                                    onUpdateSettings { it.copy(dimOpacity = finalDim) }
                                    onPreviewDimOpacity(null)
                                },
                                valueRange = AppSettings.DIM_MIN..AppSettings.DIM_MAX,
                                colors = SliderDefaults.colors(
                                    thumbColor = c.tint,
                                    activeTrackColor = c.tint,
                                    inactiveTrackColor = c.separator,
                                ),
                            )
                        }
                    }
                }
            }
        }

        // 2. Доступность
        item(key = "accessibility") {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("Доступность")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .toggleable(
                                value = settings.reduceTransparency,
                                role = Role.Switch,
                                onValueChange = { on ->
                                    view.playToggleHaptic(on)
                                    onUpdateSettings { it.copy(reduceTransparency = on) }
                                },
                            ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Уменьшить прозрачность", style = MaterialTheme.typography.bodyMedium, color = c.label)
                            Text(
                                text = "Стекло заменяется плотной заливкой",
                                style = MaterialTheme.typography.labelMedium,
                                color = c.secondaryLabel,
                            )
                        }
                        Switch(
                            checked = settings.reduceTransparency,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = c.onTint,
                                checkedTrackColor = c.tint,
                                uncheckedThumbColor = c.secondaryLabel,
                                uncheckedTrackColor = c.separator.copy(alpha = 0.35f),
                                uncheckedBorderColor = c.separator,
                            ),
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.separator))
                    SystemFlagRow("Повышенный контраст", if (highContrast) "Включён" else "Выключен")
                    SystemFlagRow("Уменьшение движения", if (reduceMotion) "Включено" else "Выключено")
                    Text(
                        text = "Системные параметры обновляются автоматически при возврате в приложение.",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.secondaryLabel,
                    )
                }
            }
        }

        // 3. О данных и расчётах
        item(key = "about") {
            val sourceLabel = rates.rates.source?.takeIf { it.isNotBlank() } ?: "Национальный Банк РК"
            val isRefreshing = rates.status == RatesStatus.Loading
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("О данных и расчётах")
                    Text(
                        text = "Курсы валют загружаются по официальным данным Национального Банка РК. " +
                            "При отсутствии сети используются последние сохранённые курсы или базовый ориентир " +
                            "(68,11 ₸ / ¥, 453,71 ₸ / $).",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.secondaryLabel,
                    )
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.separator))
                    SystemFlagRow("Источник курсов", sourceLabel)
                    SystemFlagRow("Обновление", rates.status.label)
                    SystemFlagRow(
                        "Текущие курсы",
                        "1 ¥ = ${formatRate(rates.rates.cnyKzt)} ₸ · 1 $ = ${formatRate(rates.rates.usdKzt)} ₸",
                    )
                    IosButton(
                        label = if (isRefreshing) "Обновляем курсы…" else "Обновить курсы сейчас",
                        onClick = onRefreshRates,
                        icon = Icons.Filled.Refresh,
                        enabled = !isRefreshing,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoActionsRow(
    hasPhoto: Boolean,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
) {
    val c = PddTheme.colors
    if (!hasPhoto) {
        IosButton(
            label = "Выбрать фото",
            onClick = onPickPhoto,
            icon = Icons.Filled.PhotoLibrary,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 340.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IosButton(
                    label = "Выбрать фото",
                    onClick = onPickPhoto,
                    icon = Icons.Filled.PhotoLibrary,
                    modifier = Modifier.weight(1f),
                )
                IosButton(
                    label = "Удалить фото",
                    onClick = onClearPhoto,
                    icon = Icons.Filled.Delete,
                    filled = false,
                    tint = c.danger,
                    destructive = true,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                IosButton(
                    label = "Выбрать фото",
                    onClick = onPickPhoto,
                    icon = Icons.Filled.PhotoLibrary,
                    modifier = Modifier.fillMaxWidth(),
                )
                IosButton(
                    label = "Удалить фото",
                    onClick = onClearPhoto,
                    icon = Icons.Filled.Delete,
                    filled = false,
                    tint = c.danger,
                    destructive = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SystemFlagRow(label: String, value: String) {
    val c = PddTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = c.secondaryLabel,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = c.label,
        )
    }
}

@Composable
private fun <T> Option(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = PddTheme.colors.label)
        SegmentedControl(
            options = options.map { it.second },
            selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0),
            onSelect = { onSelect(options[it].first) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
