package com.suldev.pddkz.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suldev.pddkz.ui.theme.LocalGlassState
import com.suldev.pddkz.ui.theme.LocalInlineGlassState
import com.suldev.pddkz.ui.theme.LocalReduceTransparency
import com.suldev.pddkz.ui.theme.PddTheme
import com.suldev.ui.SulGlassCard
import com.suldev.ui.SulGlassSnackbar

// Обёртки над китом: подставляют токены темы и нужный источник стекла.

/** Карточка контента. Сэмплит источник A (только фон), поэтому безопасна внутри экранов. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    deep: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = SulGlassCard(
    colors = PddTheme.colors.sul,
    state = LocalInlineGlassState.current,
    modifier = modifier,
    cornerRadius = cornerRadius,
    deep = deep,
    reduceTransparency = LocalReduceTransparency.current,
    contentPadding = contentPadding,
    onClick = onClick,
    content = content,
)

/** Снекбар. Только сиблингом источника B, не внутри экранов. */
@Composable
fun GlassSnackbar(message: String, modifier: Modifier = Modifier) = SulGlassSnackbar(
    message = message,
    state = LocalGlassState.current,
    modifier = modifier,
    reduceTransparency = LocalReduceTransparency.current,
)
