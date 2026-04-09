package com.byteflipper.random.ui.common

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.byteflipper.random.utils.findActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.showGeneratorSnackbar(
    snackbarHostState: SnackbarHostState,
    message: String
) {
    launch {
        snackbarHostState.showSnackbar(message)
    }
}

internal fun CoroutineScope.showGeneratorActionSnackbar(
    snackbarHostState: SnackbarHostState,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    launch {
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel
        )
        if (result == SnackbarResult.ActionPerformed) {
            onAction()
        }
    }
}

internal fun FlipGenerateController.runGenerateSpin(
    effectiveDelayMs: Int,
    beforeSpin: () -> Unit = {},
    onFirstOpen: () -> Unit = {},
    onReveal: (Boolean) -> Unit,
    onSpinCompleted: () -> Unit
) {
    beforeSpin()
    if (!flipController.isVisible()) {
        onFirstOpen()
        flipController.open()
    }

    isGenerating = true
    flipController.spinAndReveal(
        effectiveDelayMs = effectiveDelayMs,
        onReveal = { targetIsFront ->
            isGenerating = false
            onReveal(targetIsFront)
        },
        onSpinCompleted = onSpinCompleted
    )
}
