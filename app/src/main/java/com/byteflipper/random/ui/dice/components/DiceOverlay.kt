package com.byteflipper.random.ui.dice.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R

@Composable
fun DiceOverlay(
    scrimAlpha: Float,
    diceCount: Int,
    rotations: List<Animatable<Float, *>>,
    scales: List<Animatable<Float, *>>,
    isAnimating: List<Boolean>,
    animatedColors: List<State<Color>>,
    values: List<Int>,
    onDismiss: () -> Unit,
    onDieClick: (Int) -> Unit
) {
    BackHandler(enabled = true) { onDismiss() }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.5f * scrimAlpha))
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onDismiss() }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val rowSizes = computeDiceRowSizes(diceCount)
            val rows = rowSizes.size
            val spacing = 16.dp
            val maxInRow = rowSizes.maxOrNull() ?: 1
            val widthCandidate = (maxWidth - spacing * (maxInRow - 1)) / maxInRow
            val heightCandidate = (maxHeight - spacing * (rows - 1)) / rows
            val dieSize = kotlin.math.min(widthCandidate.value, heightCandidate.value).dp.coerceIn(84.dp, 200.dp)

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var index = 0
                repeat(rows) { rowIdx ->
                    Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(spacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val countInRow = rowSizes[rowIdx]
                        repeat(countInRow) {
                            if (index < diceCount) {
                                val i = index
                                Box(
                                    modifier = Modifier
                                        .size(dieSize)
                                        .graphicsLayer {
                                            rotationZ = rotations[i].value
                                            scaleX = scales[i].value
                                            scaleY = scales[i].value
                                        }
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            enabled = !isAnimating[i]
                                        ) {
                                            onDieClick(i)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    DiceDieFace(value = values.getOrNull(i) ?: 1, color = animatedColors[i].value, modifier = Modifier.fillMaxSize())
                                }
                                index++
                            }
                        }
                    }
                    if (rowIdx < rows - 1) Spacer(Modifier.height(spacing))
                }
                Spacer(Modifier.height(32.dp))
                val total = values.take(diceCount).sum()
                Text(
                    text = "${stringResource(R.string.sum)}: $total",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            }
        }
    }
}


