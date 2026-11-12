@file:OptIn(ExperimentalFoundationApi::class)

package com.suldev.pddkz.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialfluid.compose.materialFluidScroll
import com.suldev.kit.touch.IosOverscrollEffect
import com.suldev.kit.touch.iosOverscroll
import com.suldev.kit.touch.rememberIosOverscrollConnection
import com.suldev.kit.touch.rememberIosOverscrollEffect
import com.suldev.pddkz.ui.theme.LocalBackdropIsDark
import com.suldev.pddkz.ui.theme.LocalGlassState
import com.suldev.pddkz.ui.theme.LocalReduceMotion
import com.suldev.pddkz.ui.theme.PddTheme
import com.suldev.ui.SulLargeTitle
import com.suldev.ui.sulTopBarContentPadding

/**
 * Общий каркас экрана-вкладки: список с Large Title, уходящий под плавающие бары.
 * Отступы §5.1: поля 16 dp, между карточками 14 dp, снизу 122 dp + навбар.
 */
@Composable
fun ScreenList(
    title: String,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    overscroll: IosOverscrollEffect = rememberIosOverscrollEffect(),
    content: LazyListScope.() -> Unit,
) {
    val glassState = LocalGlassState.current
    val headerColors = PddTheme.colorsFor(LocalBackdropIsDark.current)
    val reduceMotion = LocalReduceMotion.current
    val overscrollConnection = rememberIosOverscrollConnection(overscroll)
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Системный stretch выключен: он растягивает слой и рассинхронизирует буфер стекла с карточками.
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .imePadding()
                .then(if (glassState != null) Modifier.materialFluidScroll(glassState) else Modifier)
                .iosOverscroll(overscroll, overscrollConnection),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = sulTopBarContentPadding(),
                bottom = 122.dp + navBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "largeTitle") {
                SulLargeTitle(
                    title = title,
                    listState = listState,
                    subtitle = subtitle,
                    color = headerColors.label,
                    subtitleColor = headerColors.secondaryLabel,
                    reduceMotion = reduceMotion,
                )
            }
            content()
        }
    }
}
