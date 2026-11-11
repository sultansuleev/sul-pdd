package com.suldev.pddkz.ui

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.materialfluid.compose.MaterialFluidMaterial
import com.materialfluid.compose.materialFluidPointer
import com.materialfluid.compose.materialFluidSource
import com.materialfluid.compose.rememberMaterialFluidState
import com.suldev.kit.touch.FloatingBarsTapGuard
import com.suldev.pddkz.domain.model.AppSettings
import com.suldev.pddkz.ui.components.AppBackground
import com.suldev.pddkz.ui.components.GlassSnackbar
import com.suldev.pddkz.ui.screens.CalculatorScreen
import com.suldev.pddkz.ui.screens.SettingsScreen
import com.suldev.pddkz.ui.theme.PddTheme
import com.suldev.ui.SulGlassTabBar
import com.suldev.ui.SulTab
import com.suldev.ui.SulTopBar

private val Tabs = listOf(
    SulTab("Калькулятор", Icons.Filled.Calculate),
    SulTab("Настройки", Icons.Filled.Tune),
)
private val TabTitles = listOf("Pinduoduo KZ Calc", "Настройки")

/**
 * Корень приложения, слои §3.1. Два источника стекла обязательны:
 * A (только фон) сэмплят карточки внутри контента, B (фон + контент) — бары и снекбар.
 * Один источник на всё даёт цикл RenderNode и SIGSEGV.
 */
@Composable
fun PddAppRoot(
    settings: AppSettings,
    backgroundFileName: String?,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
) {
    // Материал один на оба состояния; на отдельных компонентах не переопределяется.
    val glassState = rememberMaterialFluidState(material = settings.material)
    val inlineGlassState = rememberMaterialFluidState(material = settings.material)
    SideEffect {
        glassState.reduceTransparency = settings.reduceTransparency
        inlineGlassState.reduceTransparency = settings.reduceTransparency
    }

    var activeTab by rememberSaveable { mutableIntStateOf(0) }
    val calculatorList = rememberLazyListState()
    val settingsList = rememberLazyListState()
    val scrolled by remember {
        derivedStateOf { (if (activeTab == 0) calculatorList else settingsList).firstVisibleItemIndex > 0 }
    }
    val snackbarHostState = remember { SnackbarHostState() }

    PddTheme(
        themeMode = settings.themeMode,
        glassState = glassState,
        inlineGlassState = inlineGlassState,
        reduceTransparency = settings.reduceTransparency,
    ) {
        val c = PddTheme.colors
        SystemBarIcons(lightIcons = c.isDark)
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val background: @Composable () -> Unit = {
            AppBackground(settings.backgroundStyle, backgroundFileName, settings.dimOpacity)
        }

        Box(Modifier.fillMaxSize().materialFluidPointer(glassState)) {
            // [1] Источник A: только фон.
            Box(Modifier.matchParentSize().materialFluidSource(inlineGlassState)) { background() }

            // [2] Источник B: фон + контент вкладок.
            Box(Modifier.matchParentSize().materialFluidSource(glassState)) {
                background()
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(spring(dampingRatio = 1f, stiffness = 380f)) { direction * it / 14 } +
                            fadeIn(tween(180))) togetherWith fadeOut(tween(120))
                    },
                    label = "tabContent",
                ) { tab ->
                    when (tab) {
                        0 -> CalculatorScreen(calculatorList)
                        else -> SettingsScreen(settingsList, settings, onUpdateSettings)
                    }
                }
            }

            // [3] Перехват промахов мимо плавающего таб-бара.
            FloatingBarsTapGuard(
                totalHeight = 64.dp + navBottom + 10.dp,
                margin = 16.dp,
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            // [4] Верхний бар — сиблинг источника B.
            SulTopBar(
                title = TabTitles[activeTab],
                scrolled = scrolled,
                state = glassState,
                contentColor = c.label,
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            )

            // [5] Таб-бар — сиблинг источника B.
            SulGlassTabBar(
                tabs = Tabs,
                selectedIndex = activeTab,
                onTabSelected = { activeTab = it },
                state = glassState,
                chromeTint = c.liquidTint,
                contentColor = c.tint,
                unselectedColor = c.secondaryLabel,
                selectionColor = Color.White.copy(
                    alpha = when {
                        c.isDark -> 0.22f
                        settings.material == MaterialFluidMaterial.Liquid -> 0.38f
                        else -> 0.65f
                    },
                ),
                borderTop = c.glassHighlight,
                borderBottom = c.glassBorder,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            )

            // [6] Снекбар над таб-баром.
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { GlassSnackbar(it.visuals.message) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 96.dp + navBottom),
            )

            // [7] Шторки и диалоги — этап 5, последним сиблингом.
        }
    }
}

/** Иконки системных баров следуют теме приложения, а не системы. */
@Composable
private fun SystemBarIcons(lightIcons: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).run {
            isAppearanceLightStatusBars = !lightIcons
            isAppearanceLightNavigationBars = !lightIcons
        }
    }
}
