package com.suldev.pddkz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suldev.kit.haptics.HapticPattern
import com.suldev.kit.haptics.rememberHapticDevice
import com.suldev.pddkz.ui.theme.PddTheme
import com.suldev.ui.SulAlertDialog
import com.suldev.ui.SulSegmentedControl

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) = SulSegmentedControl(options, selectedIndex, onSelect, PddTheme.colors.sul, modifier)

@Composable
fun IOSAlertDialog(
    onDismiss: () -> Unit,
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    message: String? = null,
    dismissLabel: String? = null,
    confirmDestructive: Boolean = false,
) = SulAlertDialog(
    onDismiss = onDismiss,
    title = title,
    confirmLabel = confirmLabel,
    onConfirm = onConfirm,
    colors = PddTheme.colors.sul,
    message = message,
    dismissLabel = dismissLabel,
    confirmDestructive = confirmDestructive,
)

/** Заголовок секции: Footnote, верхний регистр; справа — необязательная метка или кнопка. */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) = Row(
    modifier = modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.6.sp),
        color = PddTheme.colors.secondaryLabel,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
    )
    trailing?.invoke()
}

/**
 * Поле ввода в заполненном iOS-стиле: подпись сверху, префикс/суффикс внутри,
 * ошибка или подсказка текстом снизу (без красной обводки).
 */
@Composable
fun IosNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    readOnly: Boolean = false,
    error: String? = null,
    hint: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val c = PddTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = c.secondaryLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .background(c.separator.copy(alpha = 0.22f))
                .border(0.5.dp, c.separator.copy(alpha = 0.4f), shape)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (prefix != null) Text(prefix, style = textStyle, color = c.secondaryLabel)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).semantics {
                    contentDescription = label
                    if (error != null) error(error)
                },
                readOnly = readOnly,
                singleLine = true,
                textStyle = textStyle.copy(color = c.label),
                cursorBrush = SolidColor(c.tint),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            )
            if (suffix != null) Text(suffix, style = MaterialTheme.typography.bodySmall, color = c.secondaryLabel)
        }
        val note = error ?: hint
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.labelMedium,
                color = if (error != null) c.danger else c.secondaryLabel,
            )
        }
    }
}

/** Компактная иконочная кнопка: круг 32 dp, тач-цель 48 dp. Сама играет тактильный отклик. */
@Composable
fun IosIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = PddTheme.colors
    val haptics = rememberHapticDevice()
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button) {
                haptics.play(HapticPattern.Click)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(c.separator.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) c.tint else c.tertiaryLabel,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
