package com.byteflipper.random.ui.ball

import android.view.SoundEffectConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.BallQuality
import com.byteflipper.random.ui.ball.components.BallAnswerCaption
import com.byteflipper.random.ui.ball.components.BallSettingsSheet
import com.byteflipper.random.ui.ball.gl.BallGlSurface
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.ui.components.ShakeEffect
import com.byteflipper.random.ui.components.SizedFab
import com.byteflipper.random.utils.TiltSensor
import com.byteflipper.random.utils.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BallScreen(
    onBack: () -> Unit,
    onOpenAnswers: () -> Unit
) {
    val viewModel: BallViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hapticsManager = LocalHapticsManager.current
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                BallUiEffect.HapticPulse -> {
                    if (settings.hapticsEnabled) {
                        hapticsManager?.performPress(settings.hapticsIntensity)
                    }
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }
                is BallUiEffect.Impact -> {
                    // Only the solid knocks are worth feeling; the nudges would blur into a buzz.
                    if (settings.hapticsEnabled && effect.strength > IMPACT_HAPTIC_THRESHOLD) {
                        hapticsManager?.performTick(settings.hapticsIntensity)
                    }
                }
                is BallUiEffect.AnswerRevealed -> {
                    if (settings.hapticsEnabled) {
                        hapticsManager?.performPress(settings.hapticsIntensity)
                    }
                    context.findActivity()?.let { activity -> viewModel.checkAd(activity) }
                }
            }
        }
    }

    // Gravity decides where the liquid settles and the movement on top of it is what a shake looks
    // like, so the sensor runs whatever the tilt setting says — the view model drops the gravity
    // stream on its own when the player has turned tilt off.
    DisposableEffect(lifecycleOwner, viewModel) {
        val tiltSensor = TiltSensor(
            context = context,
            onGravity = { gravity -> viewModel.onTilt(gravity) },
            onMotion = { acceleration -> viewModel.onMotion(acceleration) }
        )
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> tiltSensor.start()
                Lifecycle.Event.ON_PAUSE -> tiltSensor.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            tiltSensor.start()
        }
        onDispose {
            tiltSensor.stop()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Shaking the phone is how the toy has always been used.
    ShakeEffect(
        enabled = true,
        hapticsEnabled = settings.hapticsEnabled,
        hapticsIntensity = settings.hapticsIntensity,
        onShake = { viewModel.onShake() }
    )

    Scaffold(
        topBar = {
            BallTopBar(
                onBack = onBack,
                onOpenAnswers = onOpenAnswers
            )
        },
        floatingActionButton = {
            SizedFab(
                size = settings.fabSize,
                onClick = { viewModel.onEvent(BallUiEvent.ToggleSettingsSheet(true)) },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Icon(
                    painter = painterResource(R.drawable.settings_24px),
                    contentDescription = stringResource(R.string.settings)
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // The surface reaches under the bars on purpose: the gradient it draws is the screen's
            // background, and the ball is centred on the whole viewport.
            BallGlSurface(
                engine = viewModel.engine,
                onAsk = { viewModel.onEvent(BallUiEvent.Ask) },
                faceLabels = uiState.faceLabels,
                // Auto is the renderer's to decide; the other tiers are the player's word and stand.
                autoQuality = uiState.quality == BallQuality.Auto,
                topColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                bottomColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                glowColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                BallAnswerCaption(
                    phase = uiState.phase,
                    answer = uiState.answer,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 96.dp)
                )
            }
        }
    }

    BallSettingsSheet(
        visible = uiState.showSettingsSheet,
        onDismiss = { viewModel.onEvent(BallUiEvent.ToggleSettingsSheet(false)) },
        noRepeats = uiState.noRepeats,
        onNoRepeatsChange = { viewModel.onEvent(BallUiEvent.SetNoRepeats(it)) },
        tiltEnabled = uiState.tiltEnabled,
        onTiltEnabledChange = { viewModel.onEvent(BallUiEvent.SetTiltEnabled(it)) },
        quality = uiState.quality,
        onQualityChange = { viewModel.onEvent(BallUiEvent.SetQuality(it)) }
    )
}

/** Below this an impact is a nudge rather than a knock, and gets no haptic. */
private const val IMPACT_HAPTIC_THRESHOLD = 0.25f
