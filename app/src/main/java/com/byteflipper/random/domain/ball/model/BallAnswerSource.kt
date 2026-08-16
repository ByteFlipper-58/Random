package com.byteflipper.random.domain.ball.model

import com.byteflipper.random.data.settings.BALL_SOURCE_CLASSIC_ID
import com.byteflipper.random.data.settings.BALL_SOURCE_CUSTOM_ID
import kotlinx.serialization.Serializable

/**
 * Where the ball takes its answers from. The classic set is bundled with the app and localised,
 * everything else comes from the user: a saved list preset or a list typed right into the ball's
 * editor.
 */
@Serializable
sealed interface BallAnswerSource {

    /** The value persisted in settings, see [com.byteflipper.random.data.settings.Settings]. */
    val settingsId: Long

    /** The canonical 20 answers, see `ball_classic_answers`. */
    @Serializable
    data object Classic : BallAnswerSource {
        override val settingsId: Long get() = BALL_SOURCE_CLASSIC_ID
    }

    /** A saved [com.byteflipper.random.data.preset.ListPreset], referenced by its row id. */
    @Serializable
    data class Preset(val id: Long) : BallAnswerSource {
        override val settingsId: Long get() = id
    }

    /** A list stored with the ball's own settings. */
    @Serializable
    data class Custom(val items: List<String>) : BallAnswerSource {
        override val settingsId: Long get() = BALL_SOURCE_CUSTOM_ID
    }

    companion object {
        /**
         * Rebuilds the source from the two persisted fields. Preset ids are positive row ids, so a
         * value that is neither [BALL_SOURCE_CUSTOM_ID] nor positive is the classic set.
         */
        fun fromSettings(sourceId: Long, customAnswers: List<String>): BallAnswerSource = when {
            sourceId == BALL_SOURCE_CUSTOM_ID -> Custom(customAnswers)
            sourceId > 0L -> Preset(sourceId)
            else -> Classic
        }
    }
}

