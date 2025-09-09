package com.byteflipper.random.ui.coin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.domain.coin.CoinSide

@Composable
fun CoinContent(
    modifier: Modifier = Modifier,
    coinSide: CoinSide,
    rotationX: Float,
    offsetY: Float,
    bgScale: Float,
    showResult: Boolean,
    onDragThrow: () -> Unit
) {
    val density = LocalDensity.current
    Box(modifier = modifier.fillMaxSize().background(Color(0xFF4E342E))) {
        Image(
            painter = painterResource(R.drawable.desk),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val parallax = (offsetY * 0.06f).coerceIn(-24f, 24f)
                    translationY = parallax
                    scaleX = bgScale
                    scaleY = bgScale
                    alpha = 0.98f
                }
        )

        Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val coinSize = 236.dp
                val cameraDistancePx = with(density) { 96.dp.toPx() }
                val fillScale = 1.12f

                Box(
                    modifier = Modifier
                        .size(coinSize)
                        .offset(y = 12.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onDragThrow() }
                        .pointerInput(Unit) {
                            var totalDy = 0f
                            var minDy = 0f
                            var triggered = false
                            val threshold = with(density) { 16.dp.toPx() }
                            detectVerticalDragGestures(
                                onVerticalDrag = { _, dragAmount ->
                                    totalDy += dragAmount
                                    if (totalDy < minDy) minDy = totalDy
                                    if (!triggered && (minDy < -threshold || dragAmount < -threshold * 0.6f)) {
                                        triggered = true
                                        onDragThrow()
                                    }
                                },
                                onDragEnd = {
                                    totalDy = 0f
                                    minDy = 0f
                                    triggered = false
                                },
                                onDragCancel = {
                                    totalDy = 0f
                                    minDy = 0f
                                    triggered = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                this.rotationX = rotationX
                                this.translationY = offsetY
                                cameraDistance = cameraDistancePx
                                shape = CircleShape
                                clip = true
                            }
                    ) {
                        val angle = ((rotationX % 360f) + 360f) % 360f
                        val showFront = angle <= 90f || angle >= 270f

                        Image(
                            painter = painterResource(R.drawable.coin_front),
                            contentDescription = stringResource(R.string.tails),
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    scaleX = fillScale
                                    scaleY = fillScale
                                    alpha = if (showFront) 1f else 0f
                                }
                        )

                        Image(
                            painter = painterResource(R.drawable.coin_back),
                            contentDescription = stringResource(R.string.heads),
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    scaleX = fillScale
                                    scaleY = fillScale
                                    this.rotationX = 180f
                                    alpha = if (!showFront) 1f else 0f
                                }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showResult) {
                    Text(
                        text = when (coinSide) {
                            CoinSide.HEADS -> "${stringResource(R.string.result)}: ${stringResource(R.string.heads)}"
                            CoinSide.TAILS -> "${stringResource(R.string.result)}: ${stringResource(R.string.tails)}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.padding(4.dp))
                }
                Text(
                    text = stringResource(R.string.swipe_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


