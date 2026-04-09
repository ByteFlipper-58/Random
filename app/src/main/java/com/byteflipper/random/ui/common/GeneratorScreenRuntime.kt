package com.byteflipper.random.ui.common

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.byteflipper.random.ui.components.HapticsManager
import com.byteflipper.random.ui.components.LocalHapticsManager
import kotlinx.coroutines.CoroutineScope

internal class GeneratorScreenRuntime(
    val snackbarHostState: SnackbarHostState,
    val scope: CoroutineScope,
    val hapticsManager: HapticsManager?,
    val context: Context
)

@Composable
internal fun rememberGeneratorScreenRuntime(): GeneratorScreenRuntime {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val hapticsManager = LocalHapticsManager.current
    val context = LocalContext.current

    return remember(snackbarHostState, scope, hapticsManager, context) {
        GeneratorScreenRuntime(
            snackbarHostState = snackbarHostState,
            scope = scope,
            hapticsManager = hapticsManager,
            context = context
        )
    }
}
