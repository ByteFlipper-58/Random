package com.byteflipper.random.domain.ball.model

/**
 * How affirmative an answer is. Drives the reveal presentation (colour of the caption, whether
 * confetti fires) without needing per-locale metadata: for the classic set the tone follows the
 * canonical ordering, see [BallAnswerTone.forClassicIndex].
 */
enum class BallAnswerTone {
    POSITIVE,
    NEUTRAL,
    NEGATIVE;

    companion object {
        /**
         * The canonical Magic 8-Ball die carries 10 affirmative, 5 non-committal and 5 negative
         * answers, in that order. [res/values/strings.xml] keeps `ball_classic_answers` in the very
         * same order, so the tone is derivable from the index alone.
         */
        fun forClassicIndex(index: Int): BallAnswerTone = when {
            index < 10 -> POSITIVE
            index < 15 -> NEUTRAL
            else -> NEGATIVE
        }
    }
}

/** A single answer the ball can surface. */
data class BallAnswer(
    val text: String,
    val tone: BallAnswerTone
)
