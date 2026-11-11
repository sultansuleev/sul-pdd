package com.suldev.pddkz.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialfluid.compose.MaterialFluidMaterial
import com.suldev.pddkz.domain.model.AppSettings
import com.suldev.pddkz.domain.model.BackgroundStyle
import com.suldev.pddkz.domain.model.ThemeMode
import com.suldev.pddkz.ui.components.GlassCard
import com.suldev.pddkz.ui.components.ScreenList
import com.suldev.pddkz.ui.components.SectionHeader
import com.suldev.pddkz.ui.components.SegmentedControl
import com.suldev.pddkz.ui.theme.PddTheme

// Этап 3: только три переключателя оформления. Фото, затемнение, доступность — этап 6.
private val Themes = listOf(ThemeMode.SYSTEM to "Система", ThemeMode.LIGHT to "Светлая", ThemeMode.DARK to "Тёмная")
private val Materials = listOf(MaterialFluidMaterial.Liquid to "Жидкое", MaterialFluidMaterial.Frosted to "Матовое")
private val Backgrounds = listOf(BackgroundStyle.PLAIN to "Однотон", BackgroundStyle.MESH to "Меш")

@Composable
fun SettingsScreen(
    listState: LazyListState,
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
) {
    ScreenList(title = "Настройки", listState = listState) {
        item {
            SectionHeader("Оформление")
            GlassCard(Modifier.fillMaxWidth()) {
                Option("Тема оформления", Themes, settings.themeMode) { v ->
                    onUpdateSettings { it.copy(themeMode = v) }
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Option("Материал стекла", Materials, settings.material) { v ->
                    onUpdateSettings { it.copy(material = v) }
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Option("Фон экрана", Backgrounds, settings.backgroundStyle) { v ->
                    onUpdateSettings { it.copy(backgroundStyle = v) }
                }
            }
        }
    }
}

@Composable
private fun <T> Option(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Text(title, style = MaterialTheme.typography.bodyMedium, color = PddTheme.colors.label)
    SegmentedControl(
        options = options.map { it.second },
        selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0),
        onSelect = { onSelect(options[it].first) },
        modifier = Modifier.fillMaxWidth(),
    )
}
