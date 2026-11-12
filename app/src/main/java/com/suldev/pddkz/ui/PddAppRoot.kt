package com.suldev.pddkz.ui

import android.app.Activity
import android.content.Context
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.materialfluid.compose.MaterialFluidMaterial
import com.materialfluid.compose.materialFluidPointer
import com.materialfluid.compose.materialFluidSource
import com.materialfluid.compose.rememberMaterialFluidState
import com.suldev.kit.haptics.HapticPattern
import com.suldev.kit.haptics.rememberHapticDevice
import com.suldev.kit.touch.FloatingBarsTapGuard
import com.suldev.kit.touch.rememberIosOverscrollEffect
import com.suldev.pddkz.data.storage.BackgroundImageStorage
import com.suldev.pddkz.domain.model.AppSettings
import com.suldev.pddkz.domain.model.BackgroundStyle
import com.suldev.pddkz.domain.model.ThemeMode
import com.suldev.pddkz.ui.components.AppBackground
import com.suldev.pddkz.ui.components.GlassSnackbar
import com.suldev.pddkz.ui.components.IOSAlertDialog
import com.suldev.pddkz.ui.components.SheetPortal
import com.suldev.pddkz.ui.components.rememberBackgroundImage
import com.suldev.pddkz.ui.screens.CalculatorScreen
import com.suldev.pddkz.ui.screens.FormulaDetailsSheet
import com.suldev.pddkz.ui.screens.FormulaFocus
import com.suldev.pddkz.ui.screens.SettingsScreen
import com.suldev.pddkz.ui.theme.PddTheme
import com.suldev.pddkz.ui.viewmodel.PddCalculatorViewModel
import com.suldev.ui.SulGlassTabBar
import com.suldev.ui.SulTab
import com.suldev.ui.SulTopBar
import com.suldev.ui.rememberSulHeaderState
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

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
    backgroundImageStorage: BackgroundImageStorage,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    viewModel: PddCalculatorViewModel,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val calculator by viewModel.uiState.collectAsState()
    val haptics = rememberHapticDevice()
    var showResetDialog by remember { mutableStateOf(false) }
    var formulaFocus by rememberSaveable { mutableStateOf<FormulaFocus?>(null) }
    var lastFormulaFocus by rememberSaveable { mutableStateOf(FormulaFocus.ALL) }
    formulaFocus?.let { lastFormulaFocus = it }

    var highContrast by remember { mutableStateOf(readHighTextContrast(context)) }
    var reduceMotion by remember { mutableStateOf(readAnimatorScaleZero(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                highContrast = readHighTextContrast(context)
                reduceMotion = readAnimatorScaleZero(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var dimPreview by remember(settings.dimOpacity) { mutableStateOf<Float?>(null) }
    val effectiveDim = dimPreview ?: settings.dimOpacity

    val snackbarHostState = remember { SnackbarHostState() }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                backgroundImageStorage.saveFromUri(context.contentResolver, uri)
                    .onSuccess { snackbarHostState.showSnackbar("Фото фона обновлено") }
                    .onFailure { snackbarHostState.showSnackbar("Не удалось загрузить изображение") }
            }
        }
    }

    var awaitingRatesFeedback by remember { mutableStateOf(false) }
    LaunchedEffect(calculator.refreshing, awaitingRatesFeedback) {
        if (awaitingRatesFeedback && !calculator.refreshing) {
            awaitingRatesFeedback = false
            snackbarHostState.showSnackbar("Курсы НБ РК: ${calculator.rates.status.label}")
        }
    }

    // Pull-to-refresh: шайба в баре едет за сырой тягой списка, порог 80 dp.
    val calculatorOverscroll = rememberIosOverscrollEffect()
    val pullTriggerPx = with(LocalDensity.current) { 80.dp.toPx() }
    LaunchedEffect(calculatorOverscroll, pullTriggerPx) {
        // snapshotFlow вместо чтения в композиции: тяга меняется каждый кадр.
        snapshotFlow { calculatorOverscroll.rawPullY >= pullTriggerPx }
            .filter { it }
            .collect { viewModel.refreshRates() }
    }

    // Материал один на оба состояния; на отдельных компонентах не переопределяется.
    val glassState = rememberMaterialFluidState(material = settings.material)
    val inlineGlassState = rememberMaterialFluidState(material = settings.material)
    SideEffect {
        glassState.reduceTransparency = settings.reduceTransparency
        inlineGlassState.reduceTransparency = settings.reduceTransparency
        glassState.increaseContrast = highContrast
        inlineGlassState.increaseContrast = highContrast
        glassState.sampleLuminance = settings.backgroundStyle != BackgroundStyle.PLAIN
    }

    var activeTab by rememberSaveable { mutableIntStateOf(0) }
    val calculatorList = rememberLazyListState()
    val settingsList = rememberLazyListState()
    val header = rememberSulHeaderState(
        listState = if (activeTab == 0) calculatorList else settingsList,
        reduceMotion = reduceMotion,
    )
    val scrolled by remember {
        derivedStateOf { (if (activeTab == 0) calculatorList else settingsList).firstVisibleItemIndex > 0 }
    }

    val themeIsDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val useMeasuredBackdrop = settings.backgroundStyle != BackgroundStyle.PLAIN && glassState.hasBackdropLuminance
    val backdropIsDark = if (useMeasuredBackdrop) glassState.backdropIsDark else themeIsDark
    val chromeColors = PddTheme.colorsFor(backdropIsDark)
    SystemBarIcons(lightIcons = backdropIsDark)

    PddTheme(
        themeMode = settings.themeMode,
        glassState = glassState,
        inlineGlassState = inlineGlassState,
        reduceTransparency = settings.reduceTransparency,
        reduceMotion = reduceMotion,
        backdropIsDark = backdropIsDark,
    ) {
            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val backgroundImage = rememberBackgroundImage(
                style = settings.backgroundStyle,
                backgroundFileName = backgroundFileName,
                onDecodeFailed = { scope.launch { backgroundImageStorage.clear() } },
            )
            val background: @Composable () -> Unit = {
                AppBackground(settings.backgroundStyle, backgroundImage, effectiveDim)
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
                            if (reduceMotion) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                val direction = if (targetState > initialState) 1 else -1
                                (slideInHorizontally(spring(dampingRatio = 1f, stiffness = 380f)) { direction * it / 14 } +
                                    fadeIn(tween(180)) +
                                    scaleIn(
                                        initialScale = 0.985f,
                                        animationSpec = spring(dampingRatio = 1f, stiffness = 380f),
                                    )) togetherWith (fadeOut(tween(120)) +
                                    scaleOut(
                                        targetScale = 0.99f,
                                        animationSpec = spring(dampingRatio = 1f, stiffness = 380f),
                                    ))
                            }
                        },
                        label = "tabContent",
                    ) { tab ->
                        when (tab) {
                            0 -> CalculatorScreen(
                                state = calculator,
                                listState = calculatorList,
                                overscroll = calculatorOverscroll,
                                onFieldChange = viewModel::onFieldChange,
                                onCurrencyChange = viewModel::onCurrencyChange,
                                onRefreshRates = viewModel::refreshRates,
                                onOpenFormulas = { formulaFocus = it },
                            )
                            else -> SettingsScreen(
                                listState = settingsList,
                                settings = settings,
                                hasBackgroundPhoto = !backgroundFileName.isNullOrBlank(),
                                rates = calculator.rates,
                                highContrast = highContrast,
                                reduceMotion = reduceMotion,
                                onUpdateSettings = onUpdateSettings,
                                onPreviewDimOpacity = { dimPreview = it },
                                onPickPhoto = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                onClearPhoto = {
                                    scope.launch {
                                        backgroundImageStorage.clear()
                                        snackbarHostState.showSnackbar("Фото фона удалено")
                                    }
                                },
                                onRefreshRates = {
                                    awaitingRatesFeedback = true
                                    viewModel.refreshRates()
                                },
                            )
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
                    contentColor = chromeColors.label,
                    actions = if (activeTab != 0) null else {
                        {
                            IconButton(
                                onClick = {
                                    haptics.play(HapticPattern.Click)
                                    formulaFocus = FormulaFocus.ALL
                                },
                            ) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = "Формулы расчёта",
                                    tint = chromeColors.label,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(
                                onClick = {
                                    haptics.play(HapticPattern.Click)
                                    showResetDialog = true
                                },
                            ) {
                                Icon(
                                    Icons.Filled.RestartAlt,
                                    contentDescription = "Сбросить",
                                    tint = chromeColors.label,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    },
                    refreshing = activeTab == 0 && calculator.refreshing,
                    pullFraction = {
                        when {
                            activeTab != 0 -> 0f
                            calculator.refreshing -> 1f
                            else -> (calculatorOverscroll.rawPullY / pullTriggerPx).coerceIn(0f, 2f)
                        }
                    },
                    titleProgress = { header.progress },
                    reduceMotion = reduceMotion,
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                )

                // [5] Таб-бар — сиблинг источника B.
                SulGlassTabBar(
                    tabs = Tabs,
                    selectedIndex = activeTab,
                    onTabSelected = { activeTab = it },
                    state = glassState,
                    chromeTint = chromeColors.liquidTint,
                    contentColor = chromeColors.tint,
                    unselectedColor = chromeColors.secondaryLabel,
                    selectionColor = Color.White.copy(
                        alpha = when {
                            backdropIsDark -> 0.22f
                            settings.material == MaterialFluidMaterial.Liquid -> 0.38f
                            else -> 0.65f
                        },
                    ),
                    borderTop = chromeColors.glassHighlight,
                    borderBottom = chromeColors.glassBorder,
                    onDarkBackdrop = settings.backgroundStyle == BackgroundStyle.IMAGE && backdropIsDark,
                    reduceMotion = reduceMotion,
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

                // [7] Шторка формул и диалоги — последним сиблингом над хромом.
                SheetPortal(
                    visible = formulaFocus != null,
                    onDismiss = { formulaFocus = null },
                ) { scrollModifier ->
                    FormulaDetailsSheet(
                        state = calculator,
                        focus = lastFormulaFocus,
                        scrollModifier = scrollModifier,
                        onDismiss = { formulaFocus = null },
                    )
                }

                if (showResetDialog) {
                    IOSAlertDialog(
                        onDismiss = { showResetDialog = false },
                        title = "Сбросить настройки?",
                        message = "Параметры посылки, тарифы и сроки вернутся к исходным значениям. Курсы НБ РК сохранятся.",
                        confirmLabel = "Сбросить",
                        confirmDestructive = true,
                        dismissLabel = "Отмена",
                        onConfirm = {
                            viewModel.resetDefaults()
                            showResetDialog = false
                        },
                    )
                }
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

private fun readHighTextContrast(context: Context): Boolean = runCatching {
    Settings.Secure.getInt(context.contentResolver, "high_text_contrast_enabled", 0) == 1
}.getOrDefault(false)

private fun readAnimatorScaleZero(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}.getOrDefault(false)
