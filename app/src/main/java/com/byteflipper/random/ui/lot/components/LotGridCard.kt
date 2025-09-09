package com.byteflipper.random.ui.lot.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box as FoundationBox
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Offset

@Composable
fun LotGridCard(
    modifier: Modifier = Modifier,
    isRevealed: Boolean,
    isMarked: Boolean,
    cardColor: Color,
    cardSize: Dp,
    onClick: () -> Unit,
    appearDelayMs: Int = 0
) {
    val rotation = remember { Animatable(0f) }
    val appearAlpha = remember { Animatable(0f) }
    val appearScale = remember { Animatable(0.8f) }
    val target = if (isRevealed) 180f else 0f
    LaunchedEffect(target) { rotation.animateTo(target, tween(250)) }
    LaunchedEffect(appearDelayMs) {
        delay(appearDelayMs.toLong())
        appearAlpha.animateTo(1f, tween(220))
        appearScale.animateTo(1f, tween(220))
    }

    FoundationBox(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 24f * density
                alpha = appearAlpha.value
                scaleX = appearScale.value
                scaleY = appearScale.value
            }
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    !isRevealed -> cardColor
                    isMarked -> MaterialTheme.colorScheme.secondaryContainer
                    else -> Color.White
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .size(cardSize),
        contentAlignment = Alignment.Center
    ) {
        if (!isRevealed) {
            val textColor = getContrastColor(cardColor)
            val fontSize = (cardSize.value * 0.4f).coerceIn(28f, 72f).sp
            Text(
                "?",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = textColor
            )
        } else {
            if (isMarked) {
                Checkmark(cardSize)
            }
        }
    }
}

@Composable
private fun Checkmark(cardSize: Dp) {
    val color = MaterialTheme.colorScheme.primary
    val checkmarkSize = (cardSize.value * 0.12f).coerceIn(28f, 48f).dp
    val strokeWidth = (cardSize.value * 0.025f).coerceIn(5f, 10f)
    Canvas(modifier = Modifier.size(checkmarkSize).alpha(0.96f)) {
        val w = size.width
        val h = size.height
        val p1 = Offset(w * 0.82f, h * 0.64f)
        val p2 = Offset(w * 0.58f, h * 0.84f)
        val p3 = Offset(w * 0.14f, h * 0.22f)
        drawLine(color = color, start = p1, end = p2, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(color = color, start = p2, end = p3, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
}


