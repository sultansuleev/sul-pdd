package com.suldev.pddkz.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/** Заголовок секции: Footnote, верхний регистр. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) = Text(
    text = text.uppercase(),
    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.6.sp),
    color = PddTheme.colors.secondaryLabel,
    modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
)
