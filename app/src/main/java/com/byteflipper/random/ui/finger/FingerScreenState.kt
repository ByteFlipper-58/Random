package com.byteflipper.random.ui.finger

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.byteflipper.random.data.settings.FINGER_HOLD_DURATION_DEFAULT_MS
import com.byteflipper.random.data.settings.FINGER_RESULT_HOLD_SECONDS_DEFAULT
import com.byteflipper.random.data.settings.FINGER_TEAM_COUNT_DEFAULT
import com.byteflipper.random.data.settings.FINGER_WINNER_COUNT_DEFAULT
import com.byteflipper.random.data.settings.HapticsIntensity

enum class FingerMode {
    WINNER,
    TEAMS,
    ORDER
}

enum class FingerPhase {
    IDLE,
    HOLDING,
    DECIDED
}

data class TouchPoint(
    val id: Long,
    val position: Offset,
    val color: Color,
    val isWinner: Boolean = false,
    val teamIndex: Int? = null,
    val teamColor: Color? = null,
    val orderIndex: Int? = null
)

// 16 distinct, high-contrast, vibrant colors with maximum visibility across all screens and themes
val FINGER_PALETTE = listOf(
    Color(0xFFE53935), // 1. Vivid Crimson Red
    Color(0xFF1E88E5), // 2. Rich Royal Blue
    Color(0xFF43A047), // 3. Forest Emerald Green
    Color(0xFFFB8C00), // 4. Vibrant Amber Orange
    Color(0xFF8E24AA), // 5. Deep Royal Purple
    Color(0xFF00ACC1), // 6. Deep Vibrant Cyan
    Color(0xFFD81B60), // 7. Ruby Rose Pink
    Color(0xFF3949AB), // 8. Deep Indigo Blue
    Color(0xFF00897B), // 9. Teal Pine
    Color(0xFFF4511E), // 10. Deep Coral Flame
    Color(0xFF7CB342), // 11. Bright Lime Apple
    Color(0xFF039BE5), // 12. Vivid Sky Blue
    Color(0xFF7C4DFF), // 13. Electric Violet
    Color(0xFFFF4081), // 14. Hot Neon Pink
    Color(0xFF00BFA5), // 15. Seafoam Turquoise
    Color(0xFFFFB300)  // 16. Pure Amber Gold
)

// High-contrast team colors
val TEAM_COLORS = listOf(
    Color(0xFFE53935), // Team 1: Crimson Red
    Color(0xFF1E88E5), // Team 2: Royal Blue
    Color(0xFF43A047), // Team 3: Emerald Green
    Color(0xFFFB8C00)  // Team 4: Deep Orange
)

data class FingerUiState(
    val phase: FingerPhase = FingerPhase.IDLE,
    val mode: FingerMode = FingerMode.WINNER,
    val winnerCount: Int = FINGER_WINNER_COUNT_DEFAULT,
    val teamCount: Int = FINGER_TEAM_COUNT_DEFAULT,
    val holdDurationMs: Long = FINGER_HOLD_DURATION_DEFAULT_MS,
    val holdResultEnabled: Boolean = true,
    val resultHoldDurationSeconds: Int = FINGER_RESULT_HOLD_SECONDS_DEFAULT,
    val progress: Float = 0f,
    val activeFingers: Map<Long, TouchPoint> = emptyMap(),
    val isResultLocked: Boolean = false,
    val resultRemainingSeconds: Int = FINGER_RESULT_HOLD_SECONDS_DEFAULT,
    val showSettingsSheet: Boolean = false,
    val isGenerating: Boolean = false
) {
    val fingerCount: Int get() = activeFingers.size

    val minFingersNeeded: Int
        get() = when (mode) {
            FingerMode.WINNER -> (winnerCount + 1).coerceAtLeast(2)
            FingerMode.TEAMS -> teamCount.coerceAtLeast(2)
            FingerMode.ORDER -> 2
        }
}

sealed interface FingerUiEvent {
    data class PointersChanged(val pointers: List<Pair<Long, Offset>>) : FingerUiEvent
    data class SetMode(val mode: FingerMode) : FingerUiEvent
    data class SetWinnerCount(val count: Int) : FingerUiEvent
    data class SetTeamCount(val count: Int) : FingerUiEvent
    data class SetHoldDuration(val durationMs: Long) : FingerUiEvent
    data class SetHoldResultEnabled(val enabled: Boolean) : FingerUiEvent
    data class SetResultHoldDurationSeconds(val seconds: Int) : FingerUiEvent
    data class ToggleSettingsSheet(val visible: Boolean) : FingerUiEvent
    data object Reset : FingerUiEvent
}

sealed interface FingerUiEffect {
    data class HapticPulse(val intensity: HapticsIntensity = HapticsIntensity.Low) : FingerUiEffect
    data class HapticWinner(val intensity: HapticsIntensity = HapticsIntensity.High) : FingerUiEffect
    data object TriggerConfetti : FingerUiEffect
}
