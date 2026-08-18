package com.byteflipper.random.ui.ball.gl

import android.opengl.GLSurfaceView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.byteflipper.random.R
import com.byteflipper.random.domain.ball.physics.BallCommand
import com.byteflipper.random.domain.ball.physics.BallEngine
import com.byteflipper.random.ui.gl.MultisampleConfigChooser
import com.byteflipper.random.ui.gl.toRgb
import kotlin.math.min

/**
 * The ball itself: a [GLSurfaceView] driven by [BallRenderer], with the tap-to-ask and
 * drag-to-spin gestures layered on top in Compose.
 *
 * The surface is opaque, so the Compose scaffold above it (top bar, FAB, caption) composites
 * normally without `setZOrderOnTop`. [engine] is owned by the view model, which is why the
 * simulation survives the surface being recreated on a rotation.
 */
@Composable
fun BallGlSurface(
    engine: BallEngine,
    onAsk: () -> Unit,
    faceLabels: List<String>,
    autoQuality: Boolean,
    topColor: Color,
    bottomColor: Color,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val renderer = remember(engine) { BallRenderer(context.applicationContext, engine) }
    val glSurfaceView = remember(renderer) {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setEGLConfigChooser(MultisampleConfigChooser())
            preserveEGLContextOnPause = true
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    LaunchedEffect(renderer, topColor, bottomColor, glowColor) {
        renderer.setBackdropColors(topColor.toRgb(), bottomColor.toRgb(), glowColor.toRgb())
    }

    LaunchedEffect(renderer, faceLabels) {
        renderer.setAnswerLabels(faceLabels)
    }

    LaunchedEffect(renderer, autoQuality) {
        renderer.setAutoQuality(autoQuality)
    }

    DisposableEffect(lifecycleOwner, glSurfaceView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glSurfaceView.onResume()
                Lifecycle.Event.ON_PAUSE -> glSurfaceView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Still on the live GL thread here; after onPause it may be gone.
            glSurfaceView.queueEvent { renderer.release() }
            glSurfaceView.onPause()
        }
    }

    val askLabel = stringResource(R.string.ball_a11y_ask)
    val description = stringResource(R.string.ball_a11y_ball)

    // The surface starts out transparent black and the first frame is a while coming — a shader or two
    // to compile, a texture to allocate. Something ball-shaped has to be there in the meantime.
    var surfaceReady by remember(renderer) { mutableStateOf(false) }
    LaunchedEffect(renderer) {
        while (!renderer.hasDrawnFrame) withFrameNanos { }
        surfaceReady = true
    }
    val placeholderAlpha by animateFloatAsState(
        targetValue = if (surfaceReady) 0f else 1f,
        animationSpec = tween(durationMillis = PLACEHOLDER_FADE_MS),
        label = "ball_placeholder"
    )

    Box(modifier = modifier) {
        AndroidView(
            factory = { glSurfaceView },
            modifier = Modifier.matchParentSize()
        )
        if (placeholderAlpha > 0.01f) {
            BallPlaceholder(
                topColor = topColor,
                bottomColor = bottomColor,
                glowColor = glowColor,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(placeholderAlpha)
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(engine) { trackBall(engine) }
                .clickable(onClickLabel = askLabel, onClick = onAsk)
                .semantics { contentDescription = description }
        )
    }
}

/**
 * Turns touches into shell rotation: while a finger is down the ball follows it, and when the finger
 * leaves the ball keeps the speed it left at.
 *
 * The conversion is what makes that exact. A rotation of one radian carries a point on the equator
 * one radius along, so the arc a finger travels is its distance over the ball's radius on screen —
 * which is a figure only this layer knows. Everything downstream is in radians and device-independent.
 */
private suspend fun PointerInputScope.trackBall(engine: BallEngine) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val radiusPixels = (BALL_RADIUS_PER_SHORT_SIDE * min(size.width, size.height))
            .coerceAtLeast(1f)
        val radians = 1f / radiusPixels
        val velocity = VelocityTracker()
        velocity.addPointerInputChange(down)

        // Nothing is consumed until the touch is a drag rather than a tap, or asking by tapping the
        // ball would stop working.
        val dragging = awaitTouchSlopOrCancellation(down.id) { change, overSlop ->
            change.consume()
            velocity.addPointerInputChange(change)
            engine.submit(BallCommand.Grab)
            engine.submit(BallCommand.Drag(overSlop.x * radians, overSlop.y * radians))
        } ?: return@awaitEachGesture

        val completed = drag(dragging.id) { change ->
            val delta = change.positionChange()
            change.consume()
            velocity.addPointerInputChange(change)
            engine.submit(BallCommand.Drag(delta.x * radians, delta.y * radians))
        }

        // A cancelled drag (the gesture was taken over) is a release with nothing left in it.
        val flick = if (completed) velocity.calculateVelocity() else Velocity.Zero
        engine.submit(BallCommand.Fling(flick.x * radians, flick.y * radians))
    }
}

/**
 * The ball as it will look once GL has caught up: the same backdrop, a black sphere the same size in
 * the same place, one highlight where the shell's light falls. It cross-fades out rather than
 * disappearing, so the swap from a flat circle to a lit sphere is not something to notice.
 */
@Composable
private fun BallPlaceholder(
    topColor: Color,
    bottomColor: Color,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val radius = BALL_RADIUS_PER_SHORT_SIDE * min(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)

        drawRect(brush = Brush.verticalGradient(listOf(topColor, bottomColor)))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor.copy(alpha = 0.22f), Color.Transparent),
                center = center,
                radius = radius * GLOW_RADIUS_SCALE
            ),
            radius = radius * GLOW_RADIUS_SCALE,
            center = center
        )
        drawCircle(color = SHELL_COLOR, radius = radius, center = center)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(center.x - radius * 0.4f, center.y - radius * 0.45f),
                radius = radius * 0.9f
            ),
            radius = radius,
            center = center
        )
    }
}

/** Long enough to read as a dissolve, short enough not to hold the real ball up. */
private const val PLACEHOLDER_FADE_MS = 220

/** How far past the ball the backdrop's glow reaches, as a multiple of its radius. */
private const val GLOW_RADIUS_SCALE = 1.9f

/** The shell's own colour, so the placeholder and the lit sphere start from the same black. */
private val SHELL_COLOR = Color(0.045f, 0.045f, 0.052f)
