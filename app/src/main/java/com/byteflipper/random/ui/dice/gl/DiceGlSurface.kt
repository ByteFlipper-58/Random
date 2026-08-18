package com.byteflipper.random.ui.dice.gl

import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.byteflipper.random.R
import com.byteflipper.random.domain.dice.physics.DiceEngine
import com.byteflipper.random.ui.gl.MultisampleConfigChooser
import com.byteflipper.random.ui.gl.toRgb

/**
 * The dice tray: a [GLSurfaceView] driven by [DiceRenderer], with tap-to-roll layered on top in
 * Compose.
 *
 * The surface is opaque, so the scaffold above it — top bar, count selector, roll button — composites
 * normally without `setZOrderOnTop`. [engine] belongs to the view model, which is what lets a roll carry
 * on through a rotation or a trip to the recents screen.
 */
@Composable
fun DiceGlSurface(
    engine: DiceEngine,
    onRoll: () -> Unit,
    dieColors: List<Color>,
    autoQuality: Boolean,
    topColor: Color,
    bottomColor: Color,
    glowColor: Color,
    feltColor: Color,
    rimColor: Color,
    modifier: Modifier = Modifier,
    /** What the last throw came to, for TalkBack to read out once the dice have stopped. */
    resultAnnouncement: String? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnRoll = rememberUpdatedState(onRoll)
    val renderer = remember(engine) { DiceRenderer(context.applicationContext, engine) }
    val glSurfaceView = remember(renderer) {
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
        var activePointerId = MotionEvent.INVALID_POINTER_ID
        var gestureCancelled = false
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setEGLConfigChooser(MultisampleConfigChooser())
            preserveEGLContextOnPause = true
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            setOnTouchListener { surface, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        activePointerId = event.getPointerId(0)
                        gestureCancelled = false
                        surface.parent?.requestDisallowInterceptTouchEvent(true)
                        val x = event.x
                        val y = event.y
                        val time = event.eventTime
                        queueEvent { renderer.touchDown(x, y, time, touchSlop) }
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val pointer = event.findPointerIndex(activePointerId)
                        if (pointer >= 0) {
                            val x = event.getX(pointer)
                            val y = event.getY(pointer)
                            val time = event.eventTime
                            queueEvent { renderer.touchMove(x, y, time) }
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        val pointer = event.findPointerIndex(activePointerId)
                        if (!gestureCancelled && pointer >= 0) {
                            val x = event.getX(pointer)
                            val y = event.getY(pointer)
                            val time = event.eventTime
                            queueEvent { renderer.touchUp(x, y, time) }
                            surface.performClick()
                        }
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                        gestureCancelled = false
                        surface.parent?.requestDisallowInterceptTouchEvent(false)
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        queueEvent { renderer.touchCancel() }
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                        gestureCancelled = true
                        surface.parent?.requestDisallowInterceptTouchEvent(false)
                    }

                    MotionEvent.ACTION_POINTER_UP -> {
                        val pointer = event.actionIndex
                        if (event.getPointerId(pointer) == activePointerId) {
                            queueEvent { renderer.touchCancel() }
                            activePointerId = MotionEvent.INVALID_POINTER_ID
                            gestureCancelled = true
                        }
                    }
                }
                true
            }
        }
    }

    LaunchedEffect(renderer, glSurfaceView) {
        renderer.setOnEmptyTap {
            glSurfaceView.post { currentOnRoll.value() }
        }
    }

    LaunchedEffect(renderer, topColor, bottomColor, glowColor) {
        renderer.setBackdropColors(topColor.toRgb(), bottomColor.toRgb(), glowColor.toRgb())
    }

    LaunchedEffect(renderer, feltColor, rimColor) {
        renderer.setTrayColors(feltColor.toRgb(), rimColor.toRgb())
    }

    LaunchedEffect(renderer, dieColors) {
        renderer.setDieColors(dieColors.toRgb())
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

    val rollLabel = stringResource(R.string.dice_a11y_roll)
    val description = stringResource(R.string.dice_a11y_tray)

    // The surface starts out transparent black and the first frame is a few shader compiles away.
    // Something has to be there in the meantime, and black is not it.
    var surfaceReady by remember(renderer) { mutableStateOf(false) }
    LaunchedEffect(renderer) {
        while (!renderer.hasDrawnFrame) withFrameNanos { }
        surfaceReady = true
    }
    val placeholderAlpha by animateFloatAsState(
        targetValue = if (surfaceReady) 0f else 1f,
        animationSpec = tween(durationMillis = PLACEHOLDER_FADE_MS),
        label = "dice_placeholder"
    )

    Box(modifier = modifier) {
        AndroidView(
            factory = { glSurfaceView },
            modifier = Modifier.matchParentSize()
        )
        if (placeholderAlpha > 0.01f) {
            TrayPlaceholder(
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
                .semantics {
                    // The dice show their numbers on their faces, which is no help without sight, so
                    // the tray itself carries the result and says so when it changes.
                    contentDescription = resultAnnouncement ?: description
                    liveRegion = LiveRegionMode.Polite
                    onClick(label = rollLabel) {
                        currentOnRoll.value()
                        true
                    }
                }
        )
    }
}

/**
 * The backdrop the first GL frame will start from, and nothing else.
 *
 * Not the tray: where that lands on screen is a perspective projection fitted to the viewport, and a
 * tray drawn flat in roughly the right place would announce the swap it is meant to hide. The gradient
 * and its glow are the two things both passes agree on exactly, so the cross-fade has only the tray and
 * the dice to bring in — which reads as them arriving rather than as the picture changing.
 */
@Composable
private fun TrayPlaceholder(
    topColor: Color,
    bottomColor: Color,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawRect(brush = Brush.verticalGradient(listOf(topColor, bottomColor)))

        // The shader's glow is an ellipse in normalised coordinates, which on a phone held upright comes
        // out very nearly round in pixels — so one circle off the height is close enough to match it.
        val center = Offset(size.width / 2f, size.height * GLOW_CENTER_FRACTION)
        val radius = size.height * GLOW_RADIUS_FRACTION
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor.copy(alpha = GLOW_ALPHA), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}

/** Long enough to read as a dissolve, short enough not to hold the real tray up. */
private const val PLACEHOLDER_FADE_MS = 220

/** Where the backdrop's glow sits and how far it reaches, both as fractions of the viewport height. */
private const val GLOW_CENTER_FRACTION = 0.42f
private const val GLOW_RADIUS_FRACTION = 0.46f
private const val GLOW_ALPHA = 0.14f
