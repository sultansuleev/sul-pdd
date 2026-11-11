package com.suldev.pddkz.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.suldev.pddkz.ui.components.GlassCard
import com.suldev.pddkz.ui.components.ScreenList
import com.suldev.pddkz.ui.theme.PddTheme

// Заглушка этапа 3: карточки нужны, чтобы проверить стекло и прокрутку. Блоки §5.3 — этап 4.
private val PlaceholderBlocks = listOf(
    "Курсы НБ РК", "Параметры посылки", "Тарифы и сроки", "Итог сравнения",
    "Казпочта", "Карго", "Как быстро понять, что выбрать?",
)

@Composable
fun CalculatorScreen(listState: LazyListState) {
    val c = PddTheme.colors
    ScreenList(title = "Pinduoduo KZ Calc", listState = listState) {
        items(PlaceholderBlocks.size) { i ->
            GlassCard(Modifier.fillMaxWidth()) {
                Text(PlaceholderBlocks[i], style = MaterialTheme.typography.titleLarge, color = c.label)
                Text("Появится на этапе 4", style = MaterialTheme.typography.bodySmall, color = c.secondaryLabel)
            }
        }
    }
}
