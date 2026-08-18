package com.byteflipper.random.ui.dice

import android.os.SystemClock
import android.view.SoundEffectConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.SimulationQuality
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.ui.components.ShakeEffect
import com.byteflipper.random.ui.dice.components.DiceFabControls
import com.byteflipper.random.ui.dice.components.DiceOverlay
import com.byteflipper.random.ui.dice.components.DiceSettingsSheet
import com.byteflipper.random.utils.TiltSensor
import com.byteflipper.random.utils.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticsManager = LocalHapticsManager.current
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: DiceViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val controller = rememberDiceScreenController()
    val impactSounds = remember(context) { DiceImpactSoundPlayer(context.applicationContext) }

    DisposableEffect(impactSounds) {
        onDispose { impactSounds.release() }
    }

    /**
     * The flat dice and the tray are two ways of showing the same roll; this picks which.
     *
     * Null for the frame or two before settings have answered, and treated as "not the tray" by
     * everything that only has two ways to behave.
     */
    val threeDimensional = uiState.threeDimensional
    val tray = threeDimensional == true
    val tiltSensor = remember(context, viewModel) {
        TiltSensor(
            context = context,
            onMotion = { acceleration -> viewModel.onMotion(acceleration) },
            onToss = { gesture, strength ->
                view.post {
                    if (viewModel.settings.value.shakeToGenerateEnabled) {
                        if (!viewModel.uiState.value.isTrayRolling) {
                            controller.randomizeDiceColors()
                        }
                        viewModel.onTossGesture(gesture, strength)
                    }
                }
            }
        )
    }

    val animatedColors = controller.diceColors.mapIndexed { index, color ->
        animateColorAsState(
            targetValue = color,
            animationSpec = DiceAnimations.ColorChange,
            label = "dice_color_${index}"
        )
    }

    LaunchedEffect(uiState.diceCount) { controller.syncDiceCount(uiState.diceCount) }
    LaunchedEffect(controller.diceCount) { viewModel.onEvent(DiceUiEvent.SetDiceCount(controller.diceCount)) }

    // A throw left over from the flat dice must not hold the tray's screen blurred behind a scrim that
    // is no longer on it.
    LaunchedEffect(tray) {
        if (tray) {
            viewModel.onEvent(DiceUiEvent.SetOverlayVisible(false))
            controller.scrimAlpha.snapTo(0f)
        }
    }

    /** What the tray last came to rest on, for TalkBack to read. Null until a throw finishes. */
    var trayResult by remember { mutableStateOf<List<Int>?>(null) }
    LaunchedEffect(uiState.diceCount, tray) { trayResult = null }

    val rollFlat: () -> Unit = {
        controller.rollAll(
            scope = scope,
            uiState = uiState,
            settings = settings,
            viewModel = viewModel,
            view = view,
            hapticsManager = hapticsManager
        )
    }

    val rollTray: () -> Unit = {
        if (!uiState.isTrayRolling) {
            if (settings.hapticsEnabled) hapticsManager?.performPress(settings.hapticsIntensity)
            view.playSoundEffect(SoundEffectConstants.CLICK)
            controller.randomizeDiceColors()
            viewModel.onEvent(DiceUiEvent.RollTray)
        }
    }

    LaunchedEffect(tray, impactSounds) {
        if (tray) impactSounds.prepare()
    }

    // Only the tray raises these; the flat dice do their own haptics inside the roll animation.
    LaunchedEffect(viewModel) {
        var lastSoundAt = 0L
        var lastHapticAt = 0L
        viewModel.effects.collect { effect ->
            when (effect) {
                is DiceUiEffect.Impact -> {
                    val now = SystemClock.elapsedRealtime()
                    if (effect.strength >= IMPACT_SOUND_THRESHOLD &&
                        now - lastSoundAt >= IMPACT_SOUND_INTERVAL_MILLIS
                    ) {
                        impactSounds.play(effect.material, effect.strength)
                        lastSoundAt = now
                    }
                    // Only the solid knocks are worth feeling; the nudges would blur into a buzz.
                    if (effect.strength >= IMPACT_HAPTIC_THRESHOLD &&
                        now - lastHapticAt >= IMPACT_HAPTIC_INTERVAL_MILLIS &&
                        settings.hapticsEnabled
                    ) {
                        hapticsManager?.performTick(settings.hapticsIntensity)
                        lastHapticAt = now
                    }
                }
                is DiceUiEffect.Rolled -> {
                    trayResult = effect.values
                    if (settings.hapticsEnabled) {
                        hapticsManager?.performPress(settings.hapticsIntensity)
                    }
                    context.findActivity()?.let { activity -> viewModel.checkAd(activity) }
                }
            }
        }
    }

    // The tray listens for real movement only while it is visible.
    if (tray) {
        DisposableEffect(lifecycleOwner, viewModel, tiltSensor) {
            fun startSensors() {
                tiltSensor.reset()
                tiltSensor.start()
            }
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> startSensors()
                    Lifecycle.Event.ON_PAUSE -> tiltSensor.stop()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                startSensors()
            }
            onDispose {
                tiltSensor.stop()
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    ShakeEffect(
        // The 3D tray has a velocity-aware detector below the generic shake layer. Flat dice retain
        // their original shake-to-roll behaviour.
        enabled = settings.shakeToGenerateEnabled && !tray && !controller.isRolling,
        hapticsEnabled = settings.hapticsEnabled,
        hapticsIntensity = settings.hapticsIntensity,
        onShake = rollFlat
    )

    DiceScaffold(
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            DiceFabControls(
                size = settings.fabSize,
                isRolling = if (tray) uiState.isTrayRolling else controller.isRolling,
                onSettingsClick = { viewModel.onEvent(DiceUiEvent.ToggleSettingsSheet(true)) },
                onRollClick = { if (tray) rollTray() else rollFlat() }
            )
        }
    ) { inner ->
        when (threeDimensional) {
            // Neither set of dice, for the frame or two before settings have said which. The flat dice
            // and the tray look nothing alike, so drawing one on a guess is a flash of the wrong screen
            // on every entry for whoever's choice was the other one.
            null -> Unit

            true -> DiceTrayContent(
                engine = viewModel.engine,
                dieColors = controller.diceColors,
                autoQuality = uiState.quality == SimulationQuality.Auto,
                diceCount = controller.diceCount,
                onDiceCountChange = { controller.syncDiceCount(it) },
                onRoll = rollTray,
                resultAnnouncement = trayResult?.let { values ->
                    stringResource(R.string.dice_a11y_result, values.joinToString(", "))
                },
                contentPadding = inner
            )

            false -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .padding(16.dp)
                        .blur((8f * controller.scrimAlpha.value).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    DiceContent(
                        modifier = Modifier.fillMaxWidth(),
                        diceCount = controller.diceCount,
                        onDiceCountChange = { controller.syncDiceCount(it) }
                    )
                }

                if (uiState.isOverlayVisible) {
                    DiceOverlay(
                        scrimAlpha = controller.scrimAlpha.value,
                        diceCount = controller.diceCount,
                        rotations = controller.rotations,
                        scales = controller.scales,
                        isAnimating = controller.isAnimating,
                        animatedColors = animatedColors,
                        values = uiState.values,
                        onDismiss = {
                            controller.closeOverlay(scope) { visible ->
                                viewModel.onEvent(DiceUiEvent.SetOverlayVisible(visible))
                            }
                        },
                        onDieClick = { i ->
                            controller.rollSingleDie(
                                scope = scope,
                                index = i,
                                settings = settings,
                                viewModel = viewModel,
                                hapticsManager = hapticsManager
                            )
                        }
                    )
                }
            }
        }
    }

    DiceSettingsSheet(
        visible = uiState.showSettingsSheet,
        onDismiss = { viewModel.onEvent(DiceUiEvent.ToggleSettingsSheet(false)) },
        threeDimensional = tray,
        onThreeDimensionalChange = { viewModel.onEvent(DiceUiEvent.SetThreeDimensional(it)) },
        shakeToRoll = settings.shakeToGenerateEnabled,
        onShakeToRollChange = { viewModel.onEvent(DiceUiEvent.SetShakeToRoll(it)) }
    )
}

/** Below this an impact is a nudge rather than a knock, and gets no haptic. */
private const val IMPACT_HAPTIC_THRESHOLD = 0.22f
private const val IMPACT_HAPTIC_INTERVAL_MILLIS = 70L
private const val IMPACT_SOUND_THRESHOLD = 0.1f
private const val IMPACT_SOUND_INTERVAL_MILLIS = 35L
