package com.suldev.pddkz.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalPostOffice
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.suldev.pddkz.domain.formatCurrency
import com.suldev.pddkz.domain.formatPct
import com.suldev.pddkz.domain.model.ComparisonOutcome
import com.suldev.pddkz.domain.model.DisplayCurrency
import com.suldev.pddkz.ui.theme.PddTheme

val KazpostIcon: ImageVector = Icons.Filled.LocalPostOffice
val CargoIcon: ImageVector = Icons.Filled.LocalShipping

/** Плоский тонированный подблок внутри карточки: стекло в стекло не вкладывается. */
@Composable
fun TintedBlock(
    container: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .clip(shape)
            .background(container)
            .border(0.5.dp, PddTheme.colors.separator, shape)
            .padding(12.dp),
    ) { content() }
}

@Composable
private fun Capsule(text: String, color: Color, container: Color, modifier: Modifier = Modifier) = Text(
    text = text,
    style = MaterialTheme.typography.labelLarge,
    color = color,
    maxLines = 1,
    modifier = modifier.clip(CircleShape).background(container).padding(horizontal = 12.dp, vertical = 6.dp),
)

/** Hero-баннер «Результат сравнения»: тексты четырёх исходов §2.5. */
@Composable
fun WinnerBanner(
    outcome: ComparisonOutcome,
    currency: DisplayCurrency,
    rateUsdKzt: Double,
    modifier: Modifier = Modifier,
) {
    val c = PddTheme.colors
    fun money(kzt: Double) = formatCurrency(kzt, currency, rateUsdKzt)

    val icon: ImageVector
    val accent: Color
    val container: Color
    val tag: String
    val title: String
    val subtitle: String
    val badge: String
    val percent: String
    when (outcome) {
        ComparisonOutcome.NoPrice -> {
            icon = Icons.Filled.Calculate; accent = c.secondaryLabel; container = c.separator
            tag = "Анализ вариантов"
            title = "Укажите стоимость товара"
            subtitle = "Введите цену в юанях для начала расчета"
            badge = money(0.0); percent = "0% экономии"
        }
        ComparisonOutcome.Equal -> {
            icon = Icons.Filled.Balance; accent = c.secondaryLabel; container = c.separator
            tag = "Равные условия"
            title = "Стоимость одинакова!"
            subtitle = "Оба варианта выходят в абсолютно равную сумму"
            badge = money(0.0); percent = "0% разница"
        }
        is ComparisonOutcome.KazpostWins -> {
            icon = KazpostIcon; accent = c.kazpost; container = c.kazpostContainer
            tag = "Оптимальный выбор"
            title = "Выгоднее через Казпочту!"
            subtitle = "Официальная доставка с учетом НДС и комиссии дешевле"
            badge = "Экономия ${money(outcome.diffKzt)}"
            percent = "на ${formatPct(outcome.pct)}% дешевле Карго"
        }
        is ComparisonOutcome.CargoWins -> {
            icon = CargoIcon; accent = c.cargo; container = c.cargoContainer
            tag = "Оптимальный выбор"
            title = "Выгоднее через Карго!"
            subtitle = "Доставка через посредника с учетом веса выходит выгоднее"
            badge = "Экономия ${money(outcome.diffKzt)}"
            percent = "на ${formatPct(outcome.pct)}% дешевле Казпочты"
        }
    }

    GlassCard(modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(tag.uppercase(), style = MaterialTheme.typography.labelSmall, color = accent)
                Text(title, style = MaterialTheme.typography.displaySmall, color = c.label)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.secondaryLabel)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Capsule(badge, accent, container)
            Text(percent, style = MaterialTheme.typography.bodySmall, color = c.secondaryLabel)
        }
    }
}

/** Строка детализации: подпись слева, значение справа. */
class CostRow(val label: String, val value: String, val positive: Boolean = false)

/**
 * Карточка варианта доставки. [winner] — бейдж «Выгоднее»; [dimmed] — проигравший вариант
 * приглушается до 70 %.
 */
@Composable
fun DeliveryMethodCard(
    title: String,
    tag: String,
    icon: ImageVector,
    accent: Color,
    container: Color,
    daysText: String,
    total: String,
    totalYuan: String,
    rows: List<CostRow>,
    winner: Boolean,
    dimmed: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = PddTheme.colors
    val alpha by animateFloatAsState(if (dimmed) 0.70f else 1f, tween(200), label = "methodAlpha")
    GlassCard(modifier.graphicsLayer { this.alpha = alpha }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = c.label)
                Text(tag, style = MaterialTheme.typography.labelSmall, color = c.secondaryLabel)
            }
            if (winner) Capsule("Выгоднее", accent, container)
        }
        Spacer(Modifier.height(12.dp))
        TintedBlock(container, Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                Text(
                    "Срок доставки",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.secondaryLabel,
                    modifier = Modifier.weight(1f),
                )
                Text(daysText, style = MaterialTheme.typography.titleSmall, color = c.label)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Итого к оплате", style = MaterialTheme.typography.labelMedium, color = c.secondaryLabel)
        Text(total, style = MaterialTheme.typography.displayMedium, color = accent)
        Text(totalYuan, style = MaterialTheme.typography.labelSmall, color = c.secondaryLabel)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.separator))
        Spacer(Modifier.height(8.dp))
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).semantics(mergeDescendants = true) {}) {
                Text(
                    row.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.secondaryLabel,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    row.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (row.positive) c.positive else c.label,
                )
            }
        }
    }
}

/** Карточка «Как быстро понять, что выбрать?» (§2.6). */
@Composable
fun HintSection(modifier: Modifier = Modifier) {
    val c = PddTheme.colors
    GlassCard(modifier.fillMaxWidth()) {
        Text("Как быстро понять, что выбрать?", style = MaterialTheme.typography.titleLarge, color = c.label)
        Spacer(Modifier.height(12.dp))
        Hint(Icons.Filled.Inventory2, c.kazpost, c.kazpostContainer, "Тяжелые и недорогие товары") {
            append("Одежда, обувь, постельное белье. Налог Казпочты зависит от цены товара, поэтому везти тяжёлое напрямую часто ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = c.label)) { append("дешевле") }
            append(".")
        }
        Spacer(Modifier.height(12.dp))
        Hint(Icons.Filled.Devices, c.cargo, c.cargoContainer, "Легкие и дорогие товары") {
            append("Часы, электроника, гаджеты. Карго берет только за вес ($/кг). Для легких ценных вещей Карго почти всегда выгоднее.")
        }
    }
}

@Composable
private fun Hint(
    icon: ImageVector,
    accent: Color,
    container: Color,
    title: String,
    text: androidx.compose.ui.text.AnnotatedString.Builder.() -> Unit,
) {
    val c = PddTheme.colors
    TintedBlock(container, Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = c.label)
                Text(buildAnnotatedString(text), style = MaterialTheme.typography.bodySmall, color = c.secondaryLabel)
            }
        }
    }
}
