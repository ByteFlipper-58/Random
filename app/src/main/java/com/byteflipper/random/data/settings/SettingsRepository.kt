package com.byteflipper.random.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "user_settings"

private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

enum class ThemeMode(val value: Int) {
    System(0),
    Light(1),
    Dark(2);

    companion object {
        fun fromValue(value: Int?): ThemeMode = when (value) {
            1 -> Light
            2 -> Dark
            else -> System
        }
    }
}

enum class FabSizeSetting(val value: Int) {
    Small(0),
    Medium(1),
    Large(2);

    companion object {
        fun fromValue(value: Int?): FabSizeSetting = when (value) {
            0 -> Small
            2 -> Large
            else -> Medium
        }
    }
}

enum class AppLanguage(val value: Int, val localeTag: String) {
    System(0, "system"),
    English(1, "en"),
    Russian(2, "ru");

    companion object {
        fun fromValue(value: Int?): AppLanguage = when (value) {
            1 -> English
            2 -> Russian
            else -> System
        }
    }
}

data class Settings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColors: Boolean = true,
    val fabSize: FabSizeSetting = FabSizeSetting.Medium,
    val appLanguage: AppLanguage = AppLanguage.System
)

class SettingsRepository private constructor(private val appContext: Context) {

    private object Keys {
        val themeMode: Preferences.Key<Int> = intPreferencesKey("theme_mode")
        val dynamicColors: Preferences.Key<Boolean> = booleanPreferencesKey("dynamic_colors")
        val fabSize: Preferences.Key<Int> = intPreferencesKey("fab_size")
        val appLanguage: Preferences.Key<Int> = intPreferencesKey("app_language")
    }

    val settingsFlow: Flow<Settings> = appContext.dataStore.data.map { prefs ->
        Settings(
            themeMode = ThemeMode.fromValue(prefs[Keys.themeMode]),
            dynamicColors = prefs[Keys.dynamicColors] ?: true,
            fabSize = FabSizeSetting.fromValue(prefs[Keys.fabSize]),
            appLanguage = AppLanguage.fromValue(prefs[Keys.appLanguage])
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.themeMode] = mode.value
        }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.dynamicColors] = enabled
        }
    }

    suspend fun setFabSize(size: FabSizeSetting) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.fabSize] = size.value
        }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.appLanguage] = language.value
        }
    }

    companion object {
        fun fromContext(context: Context): SettingsRepository = SettingsRepository(context.applicationContext)
    }
}


