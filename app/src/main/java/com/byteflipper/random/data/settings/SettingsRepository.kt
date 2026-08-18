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

/** What to do with a wheel sector that has already come up while "no repeats" is on. */
enum class WheelUsedSectorStyle(val value: Int) {
    /** Stays on the wheel in grey, so the wheel is not re-laid out after every spin. */
    Dim(0),

    /** Leaves the wheel, and the remaining sectors spread over the freed space. */
    Remove(1);

    companion object {
        fun fromValue(value: Int?): WheelUsedSectorStyle =
            entries.firstOrNull { it.value == value } ?: Dim
    }
}

/**
 * How much a simulated generator is allowed to spend on a frame. [Auto] measures the first seconds
 * and settles on a tier by itself; the rest pin it.
 *
 * Shared by the ball of fate and the 3D dice tray: both answer the same question about the same
 * device, and one scale the player already understands beats two that mean the same thing.
 */
enum class SimulationQuality(val value: Int) {
    Auto(0),
    High(1),
    Balanced(2),
    Battery(3);

    companion object {
        fun fromValue(value: Int?): SimulationQuality = entries.firstOrNull { it.value == value } ?: Auto
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
    val setupCompleted: Boolean = false,
    val wheelNoRepeats: Boolean = false,
    val wheelSpinDurationMs: Int = WHEEL_SPIN_DURATION_DEFAULT_MS,
    val wheelUsedSectorStyle: WheelUsedSectorStyle = WheelUsedSectorStyle.Dim,
    val fingerWinnerCount: Int = FINGER_WINNER_COUNT_DEFAULT,
    val fingerTeamCount: Int = FINGER_TEAM_COUNT_DEFAULT,
    val fingerHoldDurationMs: Long = FINGER_HOLD_DURATION_DEFAULT_MS,
    val fingerHoldResultEnabled: Boolean = true,
    val fingerResultHoldDurationSeconds: Int = FINGER_RESULT_HOLD_SECONDS_DEFAULT,
    /** Preset row id, or [BALL_SOURCE_CLASSIC_ID] / [BALL_SOURCE_CUSTOM_ID]. */
    val ballAnswerSourceId: Long = BALL_SOURCE_CLASSIC_ID,
    val ballCustomAnswers: List<String> = emptyList(),
    val ballNoRepeats: Boolean = true,
    val ballTiltEnabled: Boolean = true,
    /** Off by default: the flat dice are what everyone opening the screen has used until now. */
    val dice3dEnabled: Boolean = false,
    /** One tier for every 3D scene in the app — the ball of fate and the dice tray both read it. */
    val graphicsQuality: SimulationQuality = SimulationQuality.Auto
)

/** The bundled localised set of 20 answers. */
const val BALL_SOURCE_CLASSIC_ID = 0L

/** Answers typed into the ball's own editor, stored in [Settings.ballCustomAnswers]. */
const val BALL_SOURCE_CUSTOM_ID = -1L

const val WHEEL_SPIN_DURATION_MIN_MS = 3000
const val WHEEL_SPIN_DURATION_MAX_MS = 16000
const val WHEEL_SPIN_DURATION_DEFAULT_MS = 5000

const val FINGER_WINNER_COUNT_DEFAULT = 1
const val FINGER_WINNER_COUNT_MIN = 1
const val FINGER_WINNER_COUNT_MAX = 5

const val FINGER_TEAM_COUNT_DEFAULT = 2
const val FINGER_TEAM_COUNT_MIN = 2
const val FINGER_TEAM_COUNT_MAX = 4

const val FINGER_HOLD_DURATION_DEFAULT_MS = 2500L
const val FINGER_HOLD_DURATION_MIN_MS = 1000L
const val FINGER_HOLD_DURATION_MAX_MS = 5000L

const val FINGER_RESULT_HOLD_SECONDS_DEFAULT = 3
const val FINGER_RESULT_HOLD_SECONDS_MIN = 1
const val FINGER_RESULT_HOLD_SECONDS_MAX = 10

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
        val wheelNoRepeats: Preferences.Key<Boolean> = booleanPreferencesKey("wheel_no_repeats")
        val wheelSpinDurationMs: Preferences.Key<Int> = intPreferencesKey("wheel_spin_duration_ms")
        val wheelUsedSectorStyle: Preferences.Key<Int> = intPreferencesKey("wheel_used_sector_style")
        val fingerWinnerCount: Preferences.Key<Int> = intPreferencesKey("finger_winner_count")
        val fingerTeamCount: Preferences.Key<Int> = intPreferencesKey("finger_team_count")
        val fingerHoldDurationMs: Preferences.Key<Long> = longPreferencesKey("finger_hold_duration_ms")
        val fingerHoldResultEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("finger_hold_result_enabled")
        val fingerResultHoldDurationSeconds: Preferences.Key<Int> = intPreferencesKey("finger_result_hold_duration_seconds")
        val ballAnswerSourceId: Preferences.Key<Long> = longPreferencesKey("ball_answer_source_id")
        val ballCustomAnswers: Preferences.Key<String> = stringPreferencesKey("ball_custom_answers")
        val ballNoRepeats: Preferences.Key<Boolean> = booleanPreferencesKey("ball_no_repeats")
        val ballTiltEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("ball_tilt_enabled")
        val dice3dEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("dice_3d_enabled")
        val graphicsQuality: Preferences.Key<Int> = intPreferencesKey("graphics_quality")
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
            setupCompleted = prefs[Keys.setupCompleted] ?: false,
            wheelNoRepeats = prefs[Keys.wheelNoRepeats] ?: false,
            wheelSpinDurationMs = (prefs[Keys.wheelSpinDurationMs] ?: WHEEL_SPIN_DURATION_DEFAULT_MS)
                .coerceIn(WHEEL_SPIN_DURATION_MIN_MS, WHEEL_SPIN_DURATION_MAX_MS),
            wheelUsedSectorStyle = WheelUsedSectorStyle.fromValue(prefs[Keys.wheelUsedSectorStyle]),
            fingerWinnerCount = (prefs[Keys.fingerWinnerCount] ?: FINGER_WINNER_COUNT_DEFAULT)
                .coerceIn(FINGER_WINNER_COUNT_MIN, FINGER_WINNER_COUNT_MAX),
            fingerTeamCount = (prefs[Keys.fingerTeamCount] ?: FINGER_TEAM_COUNT_DEFAULT)
                .coerceIn(FINGER_TEAM_COUNT_MIN, FINGER_TEAM_COUNT_MAX),
            fingerHoldDurationMs = (prefs[Keys.fingerHoldDurationMs] ?: FINGER_HOLD_DURATION_DEFAULT_MS)
                .coerceIn(FINGER_HOLD_DURATION_MIN_MS, FINGER_HOLD_DURATION_MAX_MS),
            fingerHoldResultEnabled = prefs[Keys.fingerHoldResultEnabled] ?: true,
            fingerResultHoldDurationSeconds = (prefs[Keys.fingerResultHoldDurationSeconds] ?: FINGER_RESULT_HOLD_SECONDS_DEFAULT)
                .coerceIn(FINGER_RESULT_HOLD_SECONDS_MIN, FINGER_RESULT_HOLD_SECONDS_MAX),
            ballAnswerSourceId = prefs[Keys.ballAnswerSourceId] ?: BALL_SOURCE_CLASSIC_ID,
            ballCustomAnswers = prefs[Keys.ballCustomAnswers]
                ?.split(Constants.ITEMS_SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?: emptyList(),
            ballNoRepeats = prefs[Keys.ballNoRepeats] ?: true,
            ballTiltEnabled = prefs[Keys.ballTiltEnabled] ?: true,
            dice3dEnabled = prefs[Keys.dice3dEnabled] ?: false,
            graphicsQuality = SimulationQuality.fromValue(prefs[Keys.graphicsQuality])
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

    suspend fun setWheelNoRepeats(enabled: Boolean) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.wheelNoRepeats] = enabled
        }
    }

    suspend fun setWheelUsedSectorStyle(style: WheelUsedSectorStyle) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.wheelUsedSectorStyle] = style.value
        }
    }

    suspend fun setWheelSpinDurationMs(durationMs: Int) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.wheelSpinDurationMs] =
                durationMs.coerceIn(WHEEL_SPIN_DURATION_MIN_MS, WHEEL_SPIN_DURATION_MAX_MS)
        }
    }

    suspend fun setFingerWinnerCount(count: Int) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.fingerWinnerCount] = count.coerceIn(FINGER_WINNER_COUNT_MIN, FINGER_WINNER_COUNT_MAX)
        }
    }

    suspend fun setFingerTeamCount(count: Int) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.fingerTeamCount] = count.coerceIn(FINGER_TEAM_COUNT_MIN, FINGER_TEAM_COUNT_MAX)
        }
    }

    suspend fun setFingerHoldDurationMs(durationMs: Long) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.fingerHoldDurationMs] = durationMs.coerceIn(FINGER_HOLD_DURATION_MIN_MS, FINGER_HOLD_DURATION_MAX_MS)
        }
    }

    suspend fun setFingerHoldResultEnabled(enabled: Boolean) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.fingerHoldResultEnabled] = enabled
        }
    }

    suspend fun setFingerResultHoldDurationSeconds(seconds: Int) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.fingerResultHoldDurationSeconds] = seconds.coerceIn(FINGER_RESULT_HOLD_SECONDS_MIN, FINGER_RESULT_HOLD_SECONDS_MAX)
        }
    }

    suspend fun setBallAnswerSourceId(sourceId: Long) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.ballAnswerSourceId] = sourceId
        }
    }

    suspend fun setBallCustomAnswers(answers: List<String>) {
        val joined = answers.filter { it.isNotBlank() }.joinToString(Constants.ITEMS_SEPARATOR)
        appContext.dataStore.edit { prefs ->
            prefs[Keys.ballCustomAnswers] = joined
        }
    }

    suspend fun setBallNoRepeats(enabled: Boolean) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.ballNoRepeats] = enabled
        }
    }

    suspend fun setBallTiltEnabled(enabled: Boolean) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.ballTiltEnabled] = enabled
        }
    }

    suspend fun setDice3dEnabled(enabled: Boolean) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.dice3dEnabled] = enabled
        }
    }

    /** The one tier both 3D scenes run at. */
    suspend fun setGraphicsQuality(quality: SimulationQuality) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.graphicsQuality] = quality.value
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


