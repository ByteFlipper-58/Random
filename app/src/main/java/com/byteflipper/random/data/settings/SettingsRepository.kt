package com.byteflipper.random.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.byteflipper.random.data.preset.transfer.PresetTransferFormat
import com.byteflipper.random.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = Constants.DATASTORE_SETTINGS_NAME)

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
    Russian(2, "ru"),
    Ukrainian(3, "uk"),
    Belarusian(4, "be"),
    Polish(5, "pl"),
    Kazakh(6, "kk"),
    Hindi(7, "hi"),
    Spanish(8, "es"),
    French(9, "fr");

    companion object {
        fun fromValue(value: Int?): AppLanguage = when (value) {
            1 -> English
            2 -> Russian
            3 -> Ukrainian
            4 -> Belarusian
            5 -> Polish
            6 -> Kazakh
            7 -> Hindi
            8 -> Spanish
            9 -> French
            else -> System
        }
    }
}

enum class HapticsIntensity(val value: Int) {
    Low(0),
    Medium(1),
    High(2);

    companion object {
        fun fromValue(value: Int?): HapticsIntensity = when (value) {
            0 -> Low
            2 -> High
            else -> Medium
        }
    }
}

data class Settings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColors: Boolean = true,
    val fabSize: FabSizeSetting = FabSizeSetting.Medium,
    val appLanguage: AppLanguage = AppLanguage.System,
    val hapticsEnabled: Boolean = true,
    val hapticsIntensity: HapticsIntensity = HapticsIntensity.Medium,
    val shakeToGenerateEnabled: Boolean = true,
    val setupCompleted: Boolean = false
)

data class ReviewPromptState(
    val firstSeenAtMs: Long = 0L,
    val sessionCount: Int = 0,
    val successfulActionCount: Int = 0,
    val lastReviewRequestAtMs: Long = 0L,
    val lastReviewRequestVersionCode: Int = 0
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
        val hapticsEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("haptics_enabled")
        val hapticsIntensity: Preferences.Key<Int> = intPreferencesKey("haptics_intensity")
        val shakeToGenerateEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("shake_to_generate_enabled")
        val setupCompleted: Preferences.Key<Boolean> = booleanPreferencesKey("setup_completed")
        val reviewFirstSeenAtMs: Preferences.Key<Long> = longPreferencesKey("review_first_seen_at_ms")
        val reviewSessionCount: Preferences.Key<Int> = intPreferencesKey("review_session_count")
        val reviewSuccessfulActionCount: Preferences.Key<Int> = intPreferencesKey("review_successful_action_count")
        val reviewLastRequestAtMs: Preferences.Key<Long> = longPreferencesKey("review_last_request_at_ms")
        val reviewLastRequestVersionCode: Preferences.Key<Int> = intPreferencesKey("review_last_request_version_code")

        // Default list storage
        val defaultListName: Preferences.Key<String> = stringPreferencesKey(Constants.DEFAULT_LIST_NAME_KEY)
        val defaultListItems: Preferences.Key<String> = stringPreferencesKey(Constants.DEFAULT_LIST_ITEMS_KEY)
        val lastPresetTransferFormat: Preferences.Key<String> = stringPreferencesKey("last_preset_transfer_format")
    }

    val settingsFlow: Flow<Settings> = appContext.dataStore.data.map { prefs ->
        Settings(
            themeMode = ThemeMode.fromValue(prefs[Keys.themeMode]),
            dynamicColors = prefs[Keys.dynamicColors] ?: true,
            fabSize = FabSizeSetting.fromValue(prefs[Keys.fabSize]),
            appLanguage = AppLanguage.fromValue(prefs[Keys.appLanguage]),
            hapticsEnabled = prefs[Keys.hapticsEnabled] ?: true,
            hapticsIntensity = HapticsIntensity.fromValue(prefs[Keys.hapticsIntensity]),
            shakeToGenerateEnabled = prefs[Keys.shakeToGenerateEnabled] ?: true,
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

    suspend fun setHapticsEnabled(enabled: Boolean) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.hapticsEnabled] = enabled
        }
    }

    suspend fun setHapticsIntensity(intensity: HapticsIntensity) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.hapticsIntensity] = intensity.value
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

    suspend fun setShakeToGenerateEnabled(enabled: Boolean) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.shakeToGenerateEnabled] = enabled
        }
    }

    suspend fun recordReviewSessionStart(startedAtMs: Long): ReviewPromptState {
        var updatedState = ReviewPromptState()
        appContext.dataStore.edit { prefs ->
            val firstSeenAtMs = prefs[Keys.reviewFirstSeenAtMs] ?: startedAtMs
            val sessionCount = (prefs[Keys.reviewSessionCount] ?: 0) + 1
            val successfulActionCount = prefs[Keys.reviewSuccessfulActionCount] ?: 0
            val lastReviewRequestAtMs = prefs[Keys.reviewLastRequestAtMs] ?: 0L
            val lastReviewRequestVersionCode = prefs[Keys.reviewLastRequestVersionCode] ?: 0

            prefs[Keys.reviewFirstSeenAtMs] = firstSeenAtMs
            prefs[Keys.reviewSessionCount] = sessionCount

            updatedState = ReviewPromptState(
                firstSeenAtMs = firstSeenAtMs,
                sessionCount = sessionCount,
                successfulActionCount = successfulActionCount,
                lastReviewRequestAtMs = lastReviewRequestAtMs,
                lastReviewRequestVersionCode = lastReviewRequestVersionCode
            )
        }
        return updatedState
    }

    suspend fun recordReviewSuccessfulAction(actionAtMs: Long): ReviewPromptState {
        var updatedState = ReviewPromptState()
        appContext.dataStore.edit { prefs ->
            val firstSeenAtMs = prefs[Keys.reviewFirstSeenAtMs] ?: actionAtMs
            val sessionCount = prefs[Keys.reviewSessionCount] ?: 0
            val successfulActionCount = (prefs[Keys.reviewSuccessfulActionCount] ?: 0) + 1
            val lastReviewRequestAtMs = prefs[Keys.reviewLastRequestAtMs] ?: 0L
            val lastReviewRequestVersionCode = prefs[Keys.reviewLastRequestVersionCode] ?: 0

            prefs[Keys.reviewFirstSeenAtMs] = firstSeenAtMs
            prefs[Keys.reviewSuccessfulActionCount] = successfulActionCount

            updatedState = ReviewPromptState(
                firstSeenAtMs = firstSeenAtMs,
                sessionCount = sessionCount,
                successfulActionCount = successfulActionCount,
                lastReviewRequestAtMs = lastReviewRequestAtMs,
                lastReviewRequestVersionCode = lastReviewRequestVersionCode
            )
        }
        return updatedState
    }

    suspend fun markReviewPromptRequested(requestedAtMs: Long, requestedVersionCode: Int) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.reviewLastRequestAtMs] = requestedAtMs
            prefs[Keys.reviewLastRequestVersionCode] = requestedVersionCode
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

    val lastPresetTransferFormatFlow: Flow<PresetTransferFormat> = appContext.dataStore.data.map { prefs ->
        when (prefs[Keys.lastPresetTransferFormat]) {
            PresetTransferFormat.Txt.name -> PresetTransferFormat.Txt
            PresetTransferFormat.Csv.name -> PresetTransferFormat.Csv
            else -> PresetTransferFormat.Json
        }
    }

    suspend fun setLastPresetTransferFormat(format: PresetTransferFormat) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.lastPresetTransferFormat] = format.name
        }
    }


}


