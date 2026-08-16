package com.byteflipper.random.domain.ball.data

import android.content.Context
import com.byteflipper.random.R
import com.byteflipper.random.domain.ball.model.BallAnswer
import com.byteflipper.random.domain.ball.model.BallAnswerSource
import com.byteflipper.random.domain.ball.model.BallAnswerTone
import com.byteflipper.random.domain.ball.physics.DieGeometry

/**
 * Turns a [BallAnswerSource] into the answers the die actually carries.
 *
 * Resources are read on every call rather than cached: the set has to follow the app locale, which
 * can change while the process lives.
 */
class BallAnswerProvider(private val context: Context) {

    /** The canonical 20 answers; the tone follows the index, see [BallAnswerTone.forClassicIndex]. */
    fun classic(): List<BallAnswer> =
        context.resources.getStringArray(R.array.ball_classic_answers)
            .mapIndexed { index, text -> BallAnswer(text, BallAnswerTone.forClassicIndex(index)) }

    /**
     * User-supplied answers. Nothing tells us how affirmative an arbitrary phrase is, so they all
     * read as neutral: no confetti, no colour hinting the outcome.
     *
     * Cut to the die's twenty faces, because every answer is printed on one of its own — a longer
     * list would have two of them sharing a face.
     */
    fun fromItems(items: List<String>): List<BallAnswer> = items
        .map(String::trim)
        .filter(String::isNotEmpty)
        .take(DieGeometry.FACE_COUNT)
        .map { BallAnswer(text = it, tone = BallAnswerTone.NEUTRAL) }

    /**
     * [presetItems] are resolved by the caller (the repository read is asynchronous). An empty or
     * missing user set falls back to the classic one so the ball is never mute.
     */
    fun resolve(source: BallAnswerSource, presetItems: List<String>? = null): List<BallAnswer> {
        val answers = when (source) {
            BallAnswerSource.Classic -> classic()
            is BallAnswerSource.Preset -> fromItems(presetItems.orEmpty())
            is BallAnswerSource.Custom -> fromItems(source.items)
        }
        return answers.ifEmpty { classic() }
    }
}
