package com.suldev.pddkz.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.materialfluid.compose.MaterialFluidState
import com.suldev.pddkz.domain.model.ThemeMode
import com.suldev.ui.SulColors

/** Цветовые токены §4.2. */
@Immutable
data class PddColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val separator: Color,
    val tint: Color,
    val onTint: Color,
    val kazpost: Color,
    val kazpostContainer: Color,
    val cargo: Color,
    val cargoContainer: Color,
    val positive: Color,
    val danger: Color,
    val warn: Color,
    val glass: Color,
    val glassDeep: Color,
    val glassHighlight: Color,
    val glassBorder: Color,
    val liquidTint: Color,
) {
    /** Тот же набор в форме, которую принимают компоненты `suldev-ui`. */
    val sul = SulColors(
        surface = surface,
        surfaceStrong = surfaceStrong,
        label = label,
        secondaryLabel = secondaryLabel,
        separator = separator,
        tint = tint,
        danger = danger,
        glass = glass,
        glassDeep = glassDeep,
        glassHighlight = glassHighlight,
        glassBorder = glassBorder,
        liquidTint = liquidTint,
    )
}

private val LightColors = PddColors(
    isDark = false,
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
    surfaceStrong = Color(0xFFFFFFFF),
    label = Color(0xFF000000),
    secondaryLabel = Color(0x993C3C43),
    tertiaryLabel = Color(0x4D3C3C43),
    separator = Color(0x333C3C43),
    tint = Color(0xFFE02E24),
    onTint = Color(0xFFFFFFFF),
    kazpost = Color(0xFF0048B3),
    kazpostContainer = Color(0x140048B3),
    cargo = Color(0xFFB45309),
    cargoContainer = Color(0x14D97706),
    positive = Color(0xFF047857),
    danger = Color(0xFFFF3B30),
    warn = Color(0xFFCC9000),
    glass = Color(0x99FFFFFF),
    glassDeep = Color(0xC8FFFFFF),
    glassHighlight = Color(0xB3FFFFFF),
    glassBorder = Color(0x66FFFFFF),
    liquidTint = Color(0x40FFFFFF),
)

private val DarkColors = PddColors(
    isDark = true,
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceStrong = Color(0xFF2C2C2E),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0x99EBEBF5),
    tertiaryLabel = Color(0x4DEBEBF5),
    separator = Color(0x33EBEBF5),
    tint = Color(0xFFFF453A),
    onTint = Color(0xFFFFFFFF),
    kazpost = Color(0xFF0A84FF),
    kazpostContainer = Color(0x1F0A84FF),
    cargo = Color(0xFFFF9F0A),
    cargoContainer = Color(0x1FFF9F0A),
    positive = Color(0xFF30D158),
    danger = Color(0xFFFF453A),
    warn = Color(0xFFFFD60A),
    glass = Color(0x2EEBEBF5),
    glassDeep = Color(0x661C1C1E),
    glassHighlight = Color(0x40FFFFFF),
    glassBorder = Color(0x20FFFFFF),
    liquidTint = Color(0x401C1C1E),
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
)

// iOS-шкала в слотах Material (§4.3).
private val LargeTitle = style(34, 40, FontWeight.Bold, -0.4)
private val Title1 = style(28, 33, FontWeight.Bold, -0.3)
private val Title2 = style(22, 27, FontWeight.Bold, -0.25)
private val Title3 = style(20, 24, FontWeight.SemiBold, -0.2)
private val Headline = style(17, 21, FontWeight.SemiBold, -0.1)
private val Emphasized15 = style(15, 19, FontWeight.SemiBold)

private val IOSTypography = Typography(
    displayLarge = LargeTitle,
    displayMedium = Title1,
    displaySmall = Title2,
    headlineLarge = Title2,
    headlineMedium = Title3,
    headlineSmall = Headline,
    titleLarge = Title3,
    titleMedium = Headline,
    titleSmall = Emphasized15,
    bodyLarge = style(17, 22, FontWeight.Normal),
    bodyMedium = style(16, 20, FontWeight.Normal),
    bodySmall = style(15, 19, FontWeight.Normal),
    labelLarge = Emphasized15,
    labelMedium = style(13, 17, FontWeight.Normal, -0.05),
    labelSmall = style(12, 15, FontWeight.Normal),
)

private val LocalPddColors = staticCompositionLocalOf { LightColors }

/** Источник B (фон + контент): его сэмплит хром — бары и снекбар. `null` → плоский фолбэк. */
val LocalGlassState = compositionLocalOf<MaterialFluidState?> { null }

/** Источник A (только фон): его сэмплят карточки внутри контента. `null` → плоский фолбэк. */
val LocalInlineGlassState = compositionLocalOf<MaterialFluidState?> { null }

/** Настройка «Уменьшить прозрачность»: стекло заменяется плотной заливкой. */
val LocalReduceTransparency = compositionLocalOf { false }

object PddTheme {
    val colors: PddColors
        @Composable @ReadOnlyComposable get() = LocalPddColors.current
}

@Composable
fun PddTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    glassState: MaterialFluidState? = null,
    inlineGlassState: MaterialFluidState? = null,
    reduceTransparency: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.tint, onPrimary = c.onTint, background = c.background, onBackground = c.label,
            surface = c.surface, onSurface = c.label, onSurfaceVariant = c.secondaryLabel,
            outline = c.separator, error = c.danger,
        )
    } else {
        lightColorScheme(
            primary = c.tint, onPrimary = c.onTint, background = c.background, onBackground = c.label,
            surface = c.surface, onSurface = c.label, onSurfaceVariant = c.secondaryLabel,
            outline = c.separator, error = c.danger,
        )
    }
    CompositionLocalProvider(
        LocalPddColors provides c,
        LocalGlassState provides glassState,
        LocalInlineGlassState provides inlineGlassState,
        LocalReduceTransparency provides reduceTransparency,
    ) {
        MaterialTheme(colorScheme = scheme, typography = IOSTypography, content = content)
    }
}
