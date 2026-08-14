package com.byteflipper.random.ui.finger

import android.view.SoundEffectConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.confetti.ConfettiEffect
import com.byteflipper.confetti.ConfettiMode
import com.byteflipper.random.R
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.ui.components.SizedFab
import com.byteflipper.random.utils.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FingerScreen(onBack: () -> Unit) {
    val viewModel: FingerViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hapticsManager = LocalHapticsManager.current
    val context = LocalContext.current
    val view = LocalView.current
    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FingerUiEffect.HapticPulse -> {
                    if (settings.hapticsEnabled) {
                        hapticsManager?.performPress(settings.hapticsIntensity)
                    }
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }
                is FingerUiEffect.HapticWinner -> {
                    if (settings.hapticsEnabled) {
                        hapticsManager?.performPress(settings.hapticsIntensity)
                    }
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }
                FingerUiEffect.TriggerConfetti -> {
                    showConfetti = true
                    context.findActivity()?.let { act -> viewModel.checkAd(act) }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            FingerTopBar(onBack = onBack)
        },
        floatingActionButton = {
            if (uiState.phase == FingerPhase.IDLE && uiState.fingerCount == 0) {
                SizedFab(
                    size = settings.fabSize,
                    onClick = { viewModel.onEvent(FingerUiEvent.ToggleSettingsSheet(true)) },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(
                        painter = painterResource(R.drawable.settings_24px),
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Fullscreen Canvas - fixed coordinate space, zero twitching on touch
            FingerCanvas(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                onPointersChanged = { pointers ->
                    viewModel.onEvent(FingerUiEvent.PointersChanged(pointers))
                }
            )

            // Floating Mode Switcher - fades out smoothly without changing Canvas layout bounds
            AnimatedVisibility(
                visible = uiState.phase == FingerPhase.IDLE && uiState.fingerCount == 0,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(150)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                FingerModeSwitcher(
                    selectedMode = uiState.mode,
                    onSelectMode = { mode ->
                        viewModel.onEvent(FingerUiEvent.SetMode(mode))
                    }
                )
            }
        }
    }

    FingerSettingsSheet(
        visible = uiState.showSettingsSheet,
        onDismiss = { viewModel.onEvent(FingerUiEvent.ToggleSettingsSheet(false)) },
        mode = uiState.mode,
        winnerCount = uiState.winnerCount,
        onWinnerCountChange = { viewModel.onEvent(FingerUiEvent.SetWinnerCount(it)) },
        teamCount = uiState.teamCount,
        onTeamCountChange = { viewModel.onEvent(FingerUiEvent.SetTeamCount(it)) },
        holdDurationMs = uiState.holdDurationMs,
        onHoldDurationChange = { viewModel.onEvent(FingerUiEvent.SetHoldDuration(it)) },
        holdResultEnabled = uiState.holdResultEnabled,
        onHoldResultEnabledChange = { viewModel.onEvent(FingerUiEvent.SetHoldResultEnabled(it)) },
        resultHoldDurationSeconds = uiState.resultHoldDurationSeconds,
        onResultHoldDurationSecondsChange = { viewModel.onEvent(FingerUiEvent.SetResultHoldDurationSeconds(it)) }
    )

    ConfettiEffect(
        trigger = showConfetti,
        mode = ConfettiMode.SIDE_CANNONS,
        particleCount = 300,
        durationMs = 3500L,
        onComplete = { showConfetti = false }
    )
}
