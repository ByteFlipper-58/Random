package com.byteflipper.random.domain.ball.usecase

import kotlin.random.Random

/**
 * Picks the answer the ball is going to show.
 *
 * The index is drawn *before* the animation starts, exactly like the wheel picks its winner up
 * front: the physics that follows only brings the chosen face to the window, it never decides the
 * outcome.
 */
class AskBallUseCase {

    data class Params(
        val answerCount: Int,
        /** Index shown by the previous ask, if any. */
        val previousIndex: Int? = null,
        /** Never draw [previousIndex] twice in a row; ignored when there is a single answer. */
        val avoidRepeat: Boolean = true,
        val random: Random = Random.Default
    )

    /** Returns the index into the answer list, or `null` when there is nothing to draw from. */
    operator fun invoke(params: Params): Int? {
        val count = params.answerCount
        if (count <= 0) return null
        if (count == 1) return 0

        val previous = params.previousIndex?.takeIf { it in 0 until count }
        if (!params.avoidRepeat || previous == null) {
            return params.random.nextInt(count)
        }

        // Draw from the remaining count and shift over the excluded index: uniform over the other
        // answers, and no retry loop.
        val drawn = params.random.nextInt(count - 1)
        return if (drawn < previous) drawn else drawn + 1
    }
}
