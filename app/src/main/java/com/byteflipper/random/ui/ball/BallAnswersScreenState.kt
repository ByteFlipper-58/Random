package com.byteflipper.random.ui.ball

import androidx.annotation.StringRes
import com.byteflipper.random.domain.ball.model.BallAnswerSource

/**
 * One row of the answers editor.
 *
 * The text alone is not enough to key a row by: two answers may read the same while they are being
 * typed, and a list keyed by position cannot animate an insert in the middle. So every row carries
 * an id that lives as long as the editor does.
 */
data class BallAnswerDraft(
    val id: Long,
    val text: String
)

data class BallAnswersUiState(
    /** What the ball is following right now, which is what [saved] was resolved from. */
    val source: BallAnswerSource = BallAnswerSource.Classic,

    /** The answers as they are stored, so the editor knows whether it has changed anything. */
    val saved: List<String> = emptyList(),

    val draft: List<BallAnswerDraft> = emptyList()
) {
    /** What a save would store: trimmed, with the blanks a half-finished edit leaves behind gone. */
    val cleaned: List<String>
        get() = draft.map { it.text.trim() }.filter(String::isNotEmpty)

    val canSave: Boolean
        get() = cleaned.size >= BALL_MIN_ANSWERS && cleaned != saved

    /** One answer per face, so at twenty the die is full. */
    val atLimit: Boolean
        get() = draft.size >= BALL_MAX_ANSWERS
}

sealed interface BallAnswersUiEffect {
    /** The new set is written; the editor has nothing left to do. */
    data object Saved : BallAnswersUiEffect

    data class ShowMessage(
        @StringRes val messageRes: Int,
        val formatArg: Int? = null
    ) : BallAnswersUiEffect
}
