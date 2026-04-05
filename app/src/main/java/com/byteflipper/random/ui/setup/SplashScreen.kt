package com.byteflipper.random.ui.setup

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.byteflipper.random.ui.theme.RandomTheme
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

private val blueCatalina: Color = Color(0xFF063773)

private const val BASE_SCALE = 1.04f
private const val BASE_SCALE_Y_MULT = 1.03f

// Скруглённый квадрат (кубик)
val CubeShape: Shape = RoundedCornerShape(16.dp)

// Хексагон (шестиугольник) со скруглёнными углами
fun RoundedHexagonShape(
    cornerRadius: Float = 24f,
    overallScale: Float = 1f,
    verticalScale: Float = 1f
): Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.5f

    val sx = overallScale
    val sy = overallScale * verticalScale

    // базовые вершины шестиугольника (до масштабирования)
    val base = listOf(
        cx to 0f,
        w to 0.25f * h,
        w to 0.75f * h,
        cx to h,
        0f to 0.75f * h,
        0f to 0.25f * h
    )

    // масштабируем относительно центра
    val pts = base.map { (x, y) ->
        val nx = cx + (x - cx) * sx
        val ny = cy + (y - cy) * sy
        nx to ny
    }

    // скругляем углы квадратичными Безье (control — сама вершина)
    val n = pts.size
    for (i in 0 until n) {
        val prev = pts[(i - 1 + n) % n]
        val v    = pts[i]
        val next = pts[(i + 1) % n]

        val dx1 = prev.first - v.first
        val dy1 = prev.second - v.second
        val dx2 = next.first - v.first
        val dy2 = next.second - v.second

        val lenPrev = sqrt(dx1 * dx1 + dy1 * dy1)
        val lenNext = sqrt(dx2 * dx2 + dy2 * dy2)

        val minLen = if (lenPrev < lenNext) lenPrev else lenNext
        val r = cornerRadius.coerceAtMost(minLen / 2f)

        val t1 = if (lenPrev > 0f) r / lenPrev else 0f
        val t2 = if (lenNext > 0f) r / lenNext else 0f

        val p1x = v.first  - (v.first  - prev.first) * t1
        val p1y = v.second - (v.second - prev.second) * t1
        val p2x = v.first  + (next.first  - v.first) * t2
        val p2y = v.second + (next.second - v.second) * t2

        if (i == 0) {
            moveTo(p1x, p1y)
        } else {
            lineTo(p1x, p1y)
        }
        quadraticTo(v.first, v.second, p2x, p2y)
    }
    close()
}

// 👉 Активная форма: без внутреннего скейла, только радиус
val ActiveShape: Shape = RoundedHexagonShape(
    cornerRadius = 32f,
    overallScale = 1f,
    verticalScale = 1f
)

/**
 * A heartbeat animation composable that displays ripple effects with an optional exit animation.
 */
@Composable
fun HeartBeatAnimation(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    exitAnimationDuration: Duration = Duration.ZERO,
    onStartExitAnimation: () -> Unit = {}
) {
    // Animation constants
    val rippleCount = 4
    val rippleDurationMs = 3333
    val rippleDelayMs = rippleDurationMs / 8
    val baseSize = 164.dp
    val containerSize = 288.dp

    // Track exit animation state
    var isExitAnimationStarted by remember { mutableStateOf(false) }

    // Trigger exit animation when visibility changes
    LaunchedEffect(isVisible) {
        if (!isVisible && !isExitAnimationStarted) {
            isExitAnimationStarted = true
            onStartExitAnimation()
        }
    }

    // Calculate screen diagonal for exit animation scaling
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val screenDiagonal = sqrt((screenWidth * screenWidth + screenHeight * screenHeight).toFloat())

    // Exit animation scale with snappy easing
    val snappyEasing = CubicBezierEasing(0.2f, 0.0f, 0.2f, 1.0f)
    val exitAnimationScale by animateFloatAsState(
        targetValue = if (isExitAnimationStarted) screenDiagonal / baseSize.value else 0f,
        animationSpec = tween(
            durationMillis = exitAnimationDuration.toInt(DurationUnit.MILLISECONDS),
            easing = snappyEasing
        ),
        label = "exitScale"
    )

    // Infinite ripple animation transition
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeatTransition")

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Only show ripples when visible and not exiting
        if (isVisible && !isExitAnimationStarted) {
            Box(
                modifier = Modifier.size(containerSize),
                contentAlignment = Alignment.Center
            ) {
                // Create ripple shapes with staggered animations
                repeat(rippleCount) { index ->
                    RippleShape(
                        infiniteTransition = infiniteTransition,
                        index = index,
                        rippleDurationMs = rippleDurationMs,
                        rippleDelayMs = rippleDelayMs,
                        baseSize = baseSize
                    )
                }
            }
        }

        // Exit animation shape
        if (isExitAnimationStarted) {
            Box(
                modifier = Modifier
                    .size(baseSize)
                    .graphicsLayer {
                        // Масштабируем фигуру: базовый скейл + выходной
                        scaleX = BASE_SCALE * exitAnimationScale
                        scaleY = BASE_SCALE * BASE_SCALE_Y_MULT * exitAnimationScale
                    }
                    .background(
                        color = blueCatalina,
                        shape = ActiveShape
                    )
            )
        }
    }
}

/**
 * Individual ripple shape component with staggered animation
 */
@Composable
private fun RippleShape(
    infiniteTransition: InfiniteTransition,
    index: Int,
    rippleDurationMs: Int,
    rippleDelayMs: Int,
    baseSize: Dp
) {
    val totalDuration = rippleDurationMs + (rippleDelayMs * index)
    val easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    // Animate scale from 1f to 4f
    val animatedScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = totalDuration,
                delayMillis = rippleDelayMs * index,
                easing = easing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale$index"
    )

    // Animate alpha from 0.25f to 0f
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = totalDuration,
                delayMillis = rippleDelayMs * index,
                easing = easing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha$index"
    )

    Box(
        modifier = Modifier
            .size(baseSize)
            .graphicsLayer {
                // Масштабируем волну: базовый скейл + анимированный
                scaleX = BASE_SCALE * animatedScale
                scaleY = BASE_SCALE * BASE_SCALE_Y_MULT * animatedScale
                alpha = animatedAlpha
            }
            .background(
                color = blueCatalina,
                shape = ActiveShape
            )
    )
}

@Preview(showBackground = true)
@Composable
fun HeartBeatAnimationPreview() {
    RandomTheme {
        HeartBeatAnimation(
            isVisible = true,
            exitAnimationDuration = 600L.milliseconds,
            onStartExitAnimation = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RippleShapePreview() {
    RandomTheme {
        RippleShape(
            infiniteTransition = rememberInfiniteTransition(label = "heartbeatTransition"),
            index = 0,
            rippleDurationMs = 3313 / 4,
            rippleDelayMs = (3313 / 4) / 8,
            baseSize = 144.dp
        )
    }
}
