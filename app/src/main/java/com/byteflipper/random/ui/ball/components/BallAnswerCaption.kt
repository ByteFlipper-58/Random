package com.byteflipper.random.ui.ball.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.domain.ball.model.BallAnswer
import com.byteflipper.random.domain.ball.model.BallAnswerTone
import com.byteflipper.random.ui.ball.BallPhase

/**
 * The answer spelled out under the ball: the text inside the window is 3D and can be read at an
 * angle, this is the part TalkBack and tired eyes rely on.
 *
 * [answer] is only passed in once the phase allows it — while the ball is still thinking the caption
 * must not give the outcome away.
 */
@Composable
fun BallAnswerCaption(
    phase: BallPhase,
    answer: BallAnswer?,
    modifier: Modifier = Modifier
) {
    val revealed = phase == BallPhase.REVEALED && answer != null

    val text = when {
        revealed -> answer.text
        phase == BallPhase.ASKING -> stringResource(R.string.ball_thinking)
        else -> stringResource(R.string.ball_hint)
    }
    val announcement = if (revealed) stringResource(R.string.ball_a11y_answer, answer.text) else null

    Text(
        text = text,
        style = if (revealed) {
            MaterialTheme.typography.headlineSmall
        } else {
            MaterialTheme.typography.bodyLarge
        },
        fontWeight = if (revealed) FontWeight.Bold else FontWeight.Normal,
        color = when {
            !revealed -> MaterialTheme.colorScheme.onSurfaceVariant
            answer.tone == BallAnswerTone.POSITIVE -> MaterialTheme.colorScheme.primary
            answer.tone == BallAnswerTone.NEGATIVE -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        },
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .animateContentSize()
            .then(
                if (announcement != null) {
                    Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = announcement
                    }
                } else {
                    Modifier
                }
            )
    )
}
