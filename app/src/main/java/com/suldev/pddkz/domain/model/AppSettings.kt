package com.suldev.pddkz.domain.model

import com.materialfluid.compose.MaterialFluidMaterial

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class BackgroundStyle { PLAIN, MESH, IMAGE }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val material: MaterialFluidMaterial = MaterialFluidMaterial.Liquid,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.PLAIN,
    val reduceTransparency: Boolean = false,
    val dimOpacity: Float = DIM_DEFAULT,
) {
    companion object {
        const val DIM_MIN = 0.15f
        const val DIM_MAX = 0.75f
        const val DIM_DEFAULT = 0.40f
    }
}
