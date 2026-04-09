package com.byteflipper.random.ui.dice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.ui.dice.components.DiceFabControls
import com.byteflipper.random.ui.dice.components.DiceOverlay
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.ui.components.ShakeEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticsManager = LocalHapticsManager.current
    val view = LocalView.current
    val viewModel: DiceViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val controller = rememberDiceScreenController()

    val animatedColors = controller.diceColors.mapIndexed { index, color ->
        animateColorAsState(
            targetValue = color,
            animationSpec = DiceAnimations.ColorChange,
            label = "dice_color_${index}"
        )
    }

    LaunchedEffect(uiState.diceCount) { controller.syncDiceCount(uiState.diceCount) }
    LaunchedEffect(controller.diceCount) { viewModel.onEvent(DiceUiEvent.SetDiceCount(controller.diceCount)) }

    // Shake-to-generate integration
    ShakeEffect(
        enabled = settings.shakeToGenerateEnabled && !controller.isRolling,
        hapticsEnabled = settings.hapticsEnabled,
        hapticsIntensity = settings.hapticsIntensity,
        onShake = {
            controller.rollAll(
                scope = scope,
                uiState = uiState,
                settings = settings,
                viewModel = viewModel,
                view = view,
                hapticsManager = hapticsManager
            )
        }
    )

    DiceScaffold(
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            DiceFabControls(
                size = settings.fabSize,
                isRolling = controller.isRolling,
                onClick = {
                    controller.rollAll(
                        scope = scope,
                        uiState = uiState,
                        settings = settings,
                        viewModel = viewModel,
                        view = view,
                        hapticsManager = hapticsManager
                    )
                }
            )
        }
    ) { inner ->
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
