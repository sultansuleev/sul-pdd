package com.suldev.pddkz.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.materialfluid.compose.MaterialFluidMaterial
import com.suldev.pddkz.domain.model.AppSettings
import com.suldev.pddkz.domain.model.BackgroundStyle
import com.suldev.pddkz.domain.model.NbkRates
import com.suldev.pddkz.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.pddDataStore by preferencesDataStore("pdd")

/** Настройки, кэш курсов и имя файла фона в одном DataStore. */
class PreferencesStorage(private val store: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = store.data.map { it.toSettings() }

    val backgroundFileName: Flow<String?> = store.data.map { it[BACKGROUND_FILE] }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[THEME] = s.themeMode.name
            prefs[MATERIAL] = s.material.name
            prefs[BACKGROUND] = s.backgroundStyle.name
            prefs[REDUCE_TRANSPARENCY] = s.reduceTransparency
            prefs[DIM] = s.dimOpacity.coerceIn(AppSettings.DIM_MIN, AppSettings.DIM_MAX)
        }
    }

    suspend fun setBackgroundFileName(name: String?) {
        store.edit { if (name == null) it.remove(BACKGROUND_FILE) else it[BACKGROUND_FILE] = name }
    }

    /** Последние успешно полученные курсы или `null`, если кэша ещё нет. */
    suspend fun cachedRates(): NbkRates? {
        val prefs = store.data.first()
        return NbkRates(
            cnyKzt = prefs[RATE_CNY] ?: return null,
            usdKzt = prefs[RATE_USD] ?: return null,
            date = prefs[RATE_DATE] ?: return null,
            source = prefs[RATE_SOURCE],
        )
    }

    suspend fun saveRates(rates: NbkRates) {
        val date = rates.date ?: return // без даты это фолбэк, а не ответ сервера
        store.edit {
            it[RATE_CNY] = rates.cnyKzt
            it[RATE_USD] = rates.usdKzt
            it[RATE_DATE] = date
            it[RATE_SOURCE] = rates.source.orEmpty()
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            themeMode = enumOr(this[THEME], d.themeMode),
            material = enumOr(this[MATERIAL], d.material),
            backgroundStyle = enumOr(this[BACKGROUND], d.backgroundStyle),
            reduceTransparency = this[REDUCE_TRANSPARENCY] ?: d.reduceTransparency,
            dimOpacity = (this[DIM] ?: d.dimOpacity).coerceIn(AppSettings.DIM_MIN, AppSettings.DIM_MAX),
        )
    }

    // Неизвестное имя (переименовали enum между версиями) не должно ронять приложение.
    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val MATERIAL = stringPreferencesKey("glass_material")
        val BACKGROUND = stringPreferencesKey("background_style")
        val REDUCE_TRANSPARENCY = booleanPreferencesKey("reduce_transparency")
        val DIM = floatPreferencesKey("dim_opacity")
        val BACKGROUND_FILE = stringPreferencesKey("background_file")
        val RATE_CNY = doublePreferencesKey("rate_cny_kzt")
        val RATE_USD = doublePreferencesKey("rate_usd_kzt")
        val RATE_DATE = stringPreferencesKey("rate_date")
        val RATE_SOURCE = stringPreferencesKey("rate_source")
    }
}
