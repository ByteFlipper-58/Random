package com.byteflipper.random.ui.ball

import com.byteflipper.random.data.settings.SimulationQuality
import com.byteflipper.random.domain.ball.model.BallAnswer
import com.byteflipper.random.domain.ball.model.BallAnswerSource
import com.byteflipper.random.domain.ball.physics.DieGeometry

/** Which die face carries answer [index]. A set longer than the die's twenty faces wraps. */
fun faceIndexFor(index: Int): Int = index % DieGeometry.FACE_COUNT

/** A ball with one answer is not a ball, it is a statement. */
const val BALL_MIN_ANSWERS = 2

/**
 * Most answers the ball can hold: the die has twenty faces and every answer is printed on its own,
 * so a twenty-first would have to share one — and sharing means the window can show a face whose
 * text is not the answer that was drawn.
 */
val BALL_MAX_ANSWERS = DieGeometry.FACE_COUNT

/**
 * Longest answer the editor accepts. The atlas shrinks text until it fits a face, so beyond this a
 * phrase would be printed too small to read in the window.
 */
const val BALL_MAX_ANSWER_LENGTH = 48

/** Where the ask is in its lifecycle; the renderer and the caption both key off this. */
enum class BallPhase {
    /** Nothing asked yet, the ball only reacts to tilt and swipes. */
    IDLE,

    /** The answer is already drawn, the die is on its way to the window. */
    ASKING,

    /** The answer is readable and stays until the next ask. */
    REVEALED
}

data class BallUiState(
    val answers: List<BallAnswer> = emptyList(),
    val source: BallAnswerSource = BallAnswerSource.Classic,
    val phase: BallPhase = BallPhase.IDLE,
    /** Index into [answers], chosen up front by `AskBallUseCase`. */
    val answerIndex: Int? = null,
    val noRepeats: Boolean = true,
    val tiltEnabled: Boolean = true,
    /** The app-wide tier, mirrored here only to tell the renderer whether Auto is in charge. */
    val quality: SimulationQuality = SimulationQuality.Auto,
    val showSettingsSheet: Boolean = false
) {
    val answer: BallAnswer? get() = answerIndex?.let { answers.getOrNull(it) }

    val canAsk: Boolean get() = answers.isNotEmpty() && phase != BallPhase.ASKING

    /**
     * What the renderer prints on each of the die's twenty faces, in face order.
     *
     * Face *i* normally carries answer *i*. A longer set wraps, so the face the current ask lands on
     * is overwritten with the answer that was actually drawn — whatever surfaces in the window then
     * always matches the caption.
     */
    val faceLabels: List<String>
        get() {
            val labels = MutableList(DieGeometry.FACE_COUNT) { face ->
                answers.getOrNull(face)?.text.orEmpty()
            }
            val drawn = answerIndex
            if (drawn != null && drawn >= DieGeometry.FACE_COUNT) {
                answers.getOrNull(drawn)?.let { labels[faceIndexFor(drawn)] = it.text }
            }
            return labels
        }
}

sealed interface BallUiEvent {
    data object Ask : BallUiEvent
    data object Reset : BallUiEvent
    data class SetNoRepeats(val enabled: Boolean) : BallUiEvent
    data class SetTiltEnabled(val enabled: Boolean) : BallUiEvent
    data class ToggleSettingsSheet(val visible: Boolean) : BallUiEvent
}

sealed interface BallUiEffect {
    /** The ask started. */
    data object HapticPulse : BallUiEffect

    /** The die knocked against the glass; [strength] is 0..1. */
    data class Impact(val strength: Float) : BallUiEffect

    data class AnswerRevealed(val answer: BallAnswer) : BallUiEffect
}
