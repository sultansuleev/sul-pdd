package com.suldev.pddkz.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suldev.pddkz.ui.theme.LocalReduceMotion
import com.suldev.pddkz.ui.theme.LocalReduceTransparency
import com.suldev.pddkz.ui.theme.PddTheme

/**
 * Портальный слой шторки (слой [7] над хромом). Обёртка над [com.suldev.ui.SheetPortal],
 * подставляющая токены темы, `reduceTransparency` и системный `reduceMotion`.
 */
@Composable
fun SheetPortal(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissThreshold: Dp = 120.dp,
    content: @Composable ColumnScope.(scrollModifier: Modifier) -> Unit,
) = SwipeDismissSheet(
    visible = visible,
    onDismiss = onDismiss,
    modifier = modifier,
    dismissThreshold = dismissThreshold,
    content = content,
)

/** Шторка с закрытием свайпом вниз (120 dp), `spring(1f, 400f)`, `BackHandler` и iOS-оверскроллом. */
@Composable
fun SwipeDismissSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissThreshold: Dp = 120.dp,
    content: @Composable ColumnScope.(scrollModifier: Modifier) -> Unit,
) {
    com.suldev.ui.SulSwipeDismissSheet(
        visible = visible,
        onDismiss = onDismiss,
        colors = PddTheme.colors.sul,
        modifier = modifier,
        dismissThreshold = dismissThreshold,
        reduceMotion = LocalReduceMotion.current,
        reduceTransparency = LocalReduceTransparency.current,
        content = content,
    )
}
