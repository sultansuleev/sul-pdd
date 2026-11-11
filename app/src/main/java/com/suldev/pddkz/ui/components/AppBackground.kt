package com.suldev.pddkz.ui.components

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.suldev.pddkz.domain.model.BackgroundStyle
import com.suldev.pddkz.ui.theme.PddTheme
import java.io.File

/** Каталог пользовательских фонов внутри `filesDir`. */
const val BACKGROUNDS_DIR = "backgrounds"

/**
 * Фон приложения. Вызывается дважды (в каждом источнике стекла) с одинаковыми аргументами.
 * Фото декодируется один раз на имя файла; если файла нет или он битый — откат к однотону.
 */
@Composable
fun AppBackground(
    style: BackgroundStyle,
    backgroundFileName: String?,
    dimOpacity: Float,
    modifier: Modifier = Modifier,
) {
    val c = PddTheme.colors
    val context = LocalContext.current
    val image = remember(style, backgroundFileName) {
        if (style != BackgroundStyle.IMAGE || backgroundFileName.isNullOrBlank()) return@remember null
        runCatching {
            val file = File(File(context.filesDir, BACKGROUNDS_DIR), backgroundFileName)
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        }.getOrNull()
    }
    com.suldev.ui.SulAppBackground(
        baseColor = c.background,
        isDark = c.isDark,
        modifier = modifier,
        mesh = style == BackgroundStyle.MESH,
        image = image,
        dimOpacity = dimOpacity,
    )
}
