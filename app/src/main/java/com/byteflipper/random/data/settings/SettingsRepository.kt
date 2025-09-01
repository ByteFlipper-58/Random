package com.byteflipper.random.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

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
    val appLanguage: AppLanguage = AppLanguage.System,
    val setupCompleted: Boolean = false
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val appContext: Context
) {

    private object Keys {
        val themeMode: Preferences.Key<Int> = intPreferencesKey("theme_mode")
        val dynamicColors: Preferences.Key<Boolean> = booleanPreferencesKey("dynamic_colors")
        val fabSize: Preferences.Key<Int> = intPreferencesKey("fab_size")
        val appLanguage: Preferences.Key<Int> = intPreferencesKey("app_language")
        val setupCompleted: Preferences.Key<Boolean> = booleanPreferencesKey("setup_completed")

        // Default list storage
        val defaultListName: Preferences.Key<String> = stringPreferencesKey("default_list_name")
        val defaultListItems: Preferences.Key<String> = stringPreferencesKey("default_list_items")
    }

    val settingsFlow: Flow<Settings> = appContext.dataStore.data.map { prefs ->
        Settings(
            themeMode = ThemeMode.fromValue(prefs[Keys.themeMode]),
            dynamicColors = prefs[Keys.dynamicColors] ?: true,
            fabSize = FabSizeSetting.fromValue(prefs[Keys.fabSize]),
            appLanguage = AppLanguage.fromValue(prefs[Keys.appLanguage]),
            setupCompleted = prefs[Keys.setupCompleted] ?: false
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

    suspend fun setSetupCompleted(completed: Boolean) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.setupCompleted] = completed
        }
    }

    // Default list methods
    val defaultListNameFlow: Flow<String?> = appContext.dataStore.data.map { prefs ->
        prefs[Keys.defaultListName]
    }

    val defaultListItemsFlow: Flow<List<String>> = appContext.dataStore.data.map { prefs ->
        prefs[Keys.defaultListItems]?.split(com.byteflipper.random.utils.Constants.ITEMS_SEPARATOR) ?: emptyList()
    }

    suspend fun getDefaultListName(): String? {
        return appContext.dataStore.data.first()[Keys.defaultListName]
    }

    suspend fun getDefaultListItems(): List<String> {
        val joinedString: String? = appContext.dataStore.data.first()[Keys.defaultListItems]
        return joinedString?.split(com.byteflipper.random.utils.Constants.ITEMS_SEPARATOR)
            ?: emptyList()
    }

    suspend fun setDefaultListName(name: String) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.defaultListName] = name
        }
    }

    suspend fun setDefaultListItems(items: List<String>) {
        val joined = items.joinToString(com.byteflipper.random.utils.Constants.ITEMS_SEPARATOR)
        appContext.dataStore.edit { prefs ->
            prefs[Keys.defaultListItems] = joined
        }
    }


}


