package com.suldev.pddkz.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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

/**
 * Капсульная кнопка в стиле iOS: минимум 48 dp по высоте, однократный отклик при клике
 * (`CONFIRM` по умолчанию, `REJECT` при [destructive] = true).
 * [filled] = true — акцентная заливка `tint`; false — нейтральная кнопка с тонкой кромкой.
 */
@Composable
fun IosButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = true,
    tint: Color? = null,
    enabled: Boolean = true,
    small: Boolean = false,
    destructive: Boolean = false,
) {
    val c = PddTheme.colors
    val view = LocalView.current
    val haptics = rememberHapticDevice()
    val accent = tint ?: c.tint
    val shape = RoundedCornerShape(50)
    val bgModifier = if (filled) {
        Modifier.background(if (enabled) accent else c.separator.copy(alpha = 0.6f))
    } else {
        Modifier
            .background(c.separator.copy(alpha = 0.22f))
            .border(0.5.dp, c.glassBorder, shape)
    }
    val contentColor = if (filled) {
        if (enabled) c.onTint else c.onTint.copy(alpha = 0.35f)
    } else {
        if (enabled) accent else c.tertiaryLabel
    }

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .then(bgModifier)
            .clickable(enabled = enabled, role = Role.Button) {
                if (destructive) {
                    haptics.play(HapticPattern.Error)
                } else {
                    val feedback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        HapticFeedbackConstants.CONFIRM
                    } else {
                        HapticFeedbackConstants.VIRTUAL_KEY
                    }
                    view.performHapticFeedback(feedback)
                }
                onClick()
            }
            .padding(
                horizontal = if (small) 14.dp else 18.dp,
                vertical = if (small) 7.dp else 12.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(if (small) 15.dp else 18.dp),
            )
        }
        Text(
            text = label,
            style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Тактильный отклик переключения тумблера (`TOGGLE_ON` / `TOGGLE_OFF` на API 34+, фолбэк `CLOCK_TICK`). */
fun android.view.View.playToggleHaptic(on: Boolean) {
    val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        if (on) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
    } else {
        HapticFeedbackConstants.CLOCK_TICK
    }
    performHapticFeedback(constant)
}
