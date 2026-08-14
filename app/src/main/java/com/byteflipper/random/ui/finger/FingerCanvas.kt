package com.byteflipper.random.ui.finger

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byteflipper.random.R
import com.byteflipper.random.ui.components.EmptyState

@Composable
fun FingerCanvas(
    modifier: Modifier = Modifier,
    uiState: FingerUiState,
    onPointersChanged: (List<Pair<Long, Offset>>) -> Unit
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val crownPath = remember { Path() }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val ambientGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_glow"
    )

    // Bouncy pop-in reveal scale for result elements
    val isDecided = uiState.phase == FingerPhase.DECIDED
    val revealScale by animateFloatAsState(
        targetValue = if (isDecided) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "reveal_scale"
    )

    val baseRadiusPx = with(density) { 38.dp.toPx() }
    val progressStrokeWidthPx = with(density) { 4.5.dp.toPx() }
    val glowRadiusPx = baseRadiusPx * 1.45f

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val activePointers = event.changes
                            .filter { it.pressed }
                            .map { it.id.value to it.position }
                        onPointersChanged(activePointers)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasCenter = center

            // Ambient background ripple on idle empty screen
            if (uiState.phase == FingerPhase.IDLE && uiState.fingerCount == 0) {
                drawCircle(
                    color = Color(0xFF1E88E5).copy(alpha = 0.04f * ambientGlowScale),
                    radius = 130.dp.toPx() * ambientGlowScale,
                    center = canvasCenter
                )
                drawCircle(
                    color = Color(0xFF8E24AA).copy(alpha = 0.03f * ambientGlowScale),
                    radius = 190.dp.toPx() * ambientGlowScale,
                    center = canvasCenter
                )
            }

            uiState.activeFingers.values.forEach { touch ->
                val center = touch.position
                val activeColor = touch.teamColor ?: touch.color

                when (uiState.phase) {
                    FingerPhase.IDLE -> {
                        // Soft outer pulse aura
                        drawCircle(
                            color = activeColor.copy(alpha = pulseAlpha * 0.4f),
                            radius = baseRadiusPx * pulseScale,
                            center = center
                        )
                        // Vibrant boundary ring
                        drawCircle(
                            color = activeColor,
                            radius = baseRadiusPx,
                            center = center,
                            style = Stroke(width = progressStrokeWidthPx)
                        )
                        // Glowing translucent core
                        drawCircle(
                            color = activeColor.copy(alpha = 0.35f),
                            radius = baseRadiusPx * 0.82f,
                            center = center
                        )
                    }

                    FingerPhase.HOLDING -> {
                        val holdPulse = 1f + (uiState.progress * 0.18f)

                        // Outer radiant energy aura
                        drawCircle(
                            color = activeColor.copy(alpha = 0.18f + uiState.progress * 0.3f),
                            radius = glowRadiusPx * holdPulse,
                            center = center
                        )

                        // Background circular track
                        drawCircle(
                            color = activeColor.copy(alpha = 0.25f),
                            radius = baseRadiusPx * 1.12f,
                            center = center,
                            style = Stroke(width = progressStrokeWidthPx)
                        )

                        // Precision progress arc
                        val sweepAngle = 360f * uiState.progress
                        drawArc(
                            color = Color.White,
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(
                                center.x - baseRadiusPx * 1.12f,
                                center.y - baseRadiusPx * 1.12f
                            ),
                            size = Size(baseRadiusPx * 2.24f, baseRadiusPx * 2.24f),
                            style = Stroke(width = progressStrokeWidthPx * 1.15f, cap = StrokeCap.Round)
                        )

                        // Solid vibrant center
                        drawCircle(
                            color = activeColor.copy(alpha = 0.92f),
                            radius = baseRadiusPx * 0.88f,
                            center = center
                        )
                    }

                    FingerPhase.DECIDED -> {
                        when (uiState.mode) {
                            FingerMode.WINNER -> {
                                if (touch.isWinner) {
                                    val winnerRadius = baseRadiusPx * 1.25f * revealScale
                                    val goldColor = Color(0xFFFFD700)
                                    val goldBorderColor = Color(0xFFD4AF37)
                                    val medallionRadius = winnerRadius * 0.58f

                                    // Luminous golden shockwave
                                    drawCircle(
                                        color = goldColor.copy(alpha = pulseAlpha * 0.6f),
                                        radius = winnerRadius * pulseScale * 1.22f,
                                        center = center
                                    )
                                    // Deep luminous body
                                    drawCircle(
                                        color = activeColor,
                                        radius = winnerRadius,
                                        center = center
                                    )
                                    // Gold victory border
                                    drawCircle(
                                        color = goldColor,
                                        radius = winnerRadius,
                                        center = center,
                                        style = Stroke(width = with(density) { 4.dp.toPx() })
                                    )
                                    // Inner white contour
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.85f),
                                        radius = winnerRadius * 0.84f,
                                        center = center,
                                        style = Stroke(width = with(density) { 1.5.dp.toPx() })
                                    )

                                    // Clean solid white medallion in center
                                    drawCircle(
                                        color = Color.White,
                                        radius = medallionRadius,
                                        center = center
                                    )

                                    // Mathematical precision royal 3-point Crown with pop-in scale
                                    val crownW = with(density) { 24.dp.toPx() } * revealScale
                                    val crownH = with(density) { 18.dp.toPx() } * revealScale

                                    val leftPeakX = center.x - crownW * 0.44f
                                    val leftPeakY = center.y - crownH * 0.22f
                                    val centerPeakX = center.x
                                    val centerPeakY = center.y - crownH * 0.48f
                                    val rightPeakX = center.x + crownW * 0.44f
                                    val rightPeakY = center.y - crownH * 0.22f

                                    buildCrown(crownPath, center, crownW, crownH)
                                    drawPath(
                                        path = crownPath,
                                        color = goldColor
                                    )
                                    drawPath(
                                        path = crownPath,
                                        color = goldBorderColor,
                                        style = Stroke(width = with(density) { 1.2.dp.toPx() })
                                    )

                                    // Crown Pearls perfectly centered on spike vertices
                                    val pearlRadius = with(density) { 2.dp.toPx() } * revealScale
                                    drawCircle(goldColor, pearlRadius, Offset(centerPeakX, centerPeakY))
                                    drawCircle(goldBorderColor, pearlRadius, Offset(centerPeakX, centerPeakY), style = Stroke(width = with(density) { 0.8.dp.toPx() }))

                                    drawCircle(goldColor, pearlRadius, Offset(leftPeakX, leftPeakY))
                                    drawCircle(goldBorderColor, pearlRadius, Offset(leftPeakX, leftPeakY), style = Stroke(width = with(density) { 0.8.dp.toPx() }))

                                    drawCircle(goldColor, pearlRadius, Offset(rightPeakX, rightPeakY))
                                    drawCircle(goldBorderColor, pearlRadius, Offset(rightPeakX, rightPeakY), style = Stroke(width = with(density) { 0.8.dp.toPx() }))
                                } else {
                                    // Soft dimmed loser
                                    val loserRadius = baseRadiusPx * 0.68f
                                    drawCircle(
                                        color = activeColor.copy(alpha = 0.10f),
                                        radius = loserRadius,
                                        center = center
                                    )
                                    drawCircle(
                                        color = Color.Gray.copy(alpha = 0.22f),
                                        radius = loserRadius,
                                        center = center,
                                        style = Stroke(
                                            width = with(density) { 1.5.dp.toPx() },
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                        )
                                    )
                                }
                            }

                            FingerMode.TEAMS -> {
                                val teamRadius = baseRadiusPx * 1.15f * revealScale
                                val teamCol = touch.teamColor ?: activeColor
                                val badgeRadius = teamRadius * 0.54f

                                // Radiant team glow
                                drawCircle(
                                    color = teamCol.copy(alpha = pulseAlpha * 0.4f),
                                    radius = teamRadius * pulseScale,
                                    center = center
                                )
                                // Solid team body
                                drawCircle(
                                    color = teamCol,
                                    radius = teamRadius,
                                    center = center
                                )
                                // Thick white team border ring
                                drawCircle(
                                    color = Color.White,
                                    radius = teamRadius,
                                    center = center,
                                    style = Stroke(width = with(density) { 3.5.dp.toPx() })
                                )

                                // Solid white center medallion
                                drawCircle(
                                    color = Color.White,
                                    radius = badgeRadius,
                                    center = center
                                )

                                // Team number inside white medallion
                                val numText = (touch.teamIndex ?: 1).toString()
                                val textLayout = textMeasurer.measure(
                                    text = numText,
                                    style = TextStyle(
                                        color = teamCol,
                                        fontSize = (20 * revealScale).sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                drawText(
                                    textLayoutResult = textLayout,
                                    topLeft = Offset(
                                        center.x - textLayout.size.width / 2f,
                                        center.y - textLayout.size.height / 2f
                                    )
                                )
                            }

                            FingerMode.ORDER -> {
                                val orderRadius = baseRadiusPx * 1.15f * revealScale
                                val orderIdx = touch.orderIndex ?: 1
                                val badgeRadius = orderRadius * 0.54f

                                val rankColor = when (orderIdx) {
                                    1 -> Color(0xFFFFD700) // Gold for #1
                                    2 -> Color(0xFFDCDCDC) // Silver for #2
                                    3 -> Color(0xFFCD7F32) // Bronze for #3
                                    else -> Color(0xFF1E88E5) // Royal Blue for #4+
                                }

                                // Metallic ranked glow
                                drawCircle(
                                    color = rankColor.copy(alpha = pulseAlpha * 0.45f),
                                    radius = orderRadius * pulseScale,
                                    center = center
                                )
                                // Solid ranked body
                                drawCircle(
                                    color = rankColor,
                                    radius = orderRadius,
                                    center = center
                                )
                                // White rank border
                                drawCircle(
                                    color = Color.White,
                                    radius = orderRadius,
                                    center = center,
                                    style = Stroke(width = with(density) { 3.5.dp.toPx() })
                                )

                                // Solid white center medallion
                                drawCircle(
                                    color = Color.White,
                                    radius = badgeRadius,
                                    center = center
                                )

                                // Order number inside white medallion
                                val numText = orderIdx.toString()
                                val textLayout = textMeasurer.measure(
                                    text = numText,
                                    style = TextStyle(
                                        color = rankColor,
                                        fontSize = (20 * revealScale).sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                drawText(
                                    textLayoutResult = textLayout,
                                    topLeft = Offset(
                                        center.x - textLayout.size.width / 2f,
                                        center.y - textLayout.size.height / 2f
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Clean Empty State in center (visible ONLY when 0 fingers are on screen)
        if (uiState.phase == FingerPhase.IDLE && uiState.fingerCount == 0) {
            EmptyState(
                iconRes = R.drawable.touch_app_24px,
                title = stringResource(R.string.finger),
                description = stringResource(R.string.finger_touch_hint),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private fun buildCrown(
    path: Path,
    center: Offset,
    width: Float,
    height: Float
) {
    val leftPeakX = center.x - width * 0.44f
    val leftPeakY = center.y - height * 0.22f

    val centerPeakX = center.x
    val centerPeakY = center.y - height * 0.48f

    val rightPeakX = center.x + width * 0.44f
    val rightPeakY = center.y - height * 0.22f

    val bottomY = center.y + height * 0.42f
    val valleyY = center.y - height * 0.02f
    val valleyInsetX = width * 0.22f
    val baseInsetX = width * 0.38f

    path.reset()
    // Bottom base left
    path.moveTo(center.x - baseInsetX, bottomY)
    // Bottom base right
    path.lineTo(center.x + baseInsetX, bottomY)
    // Up to right peak
    path.lineTo(rightPeakX, rightPeakY)
    // Inward right valley
    path.lineTo(center.x + valleyInsetX, valleyY)
    // Highest center peak
    path.lineTo(centerPeakX, centerPeakY)
    // Inward left valley
    path.lineTo(center.x - valleyInsetX, valleyY)
    // Up to left peak
    path.lineTo(leftPeakX, leftPeakY)
    path.close()
}
