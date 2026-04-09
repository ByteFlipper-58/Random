package com.byteflipper.random.ui.common

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.byteflipper.random.ui.components.HapticsManager
import kotlinx.coroutines.flow.Flow

@Composable
internal fun CollectFeedbackEffects(
    effects: Flow<FeedbackUiEffect>,
    snackbarHostState: SnackbarHostState,
    context: Context,
    hapticsManager: HapticsManager?
) {
    LaunchedEffect(effects, snackbarHostState, context, hapticsManager) {
        effects.collect { effect ->
            effect.snackbarMessageRes?.let { messageRes ->
                snackbarHostState.showSnackbar(context.getString(messageRes))
            }
            effect.hapticIntensity?.let { intensity ->
                hapticsManager?.performPress(intensity)
            }
        }
    }
}
