package com.byteflipper.random.ui.teams

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsScaffold(
    onBack: () -> Unit,
    onOpenPreset: () -> Unit,
    hasPresets: Boolean,
    onManagePeople: () -> Unit,
    onSavePreset: () -> Unit,
    snackbarHostState: SnackbarHostState,
    floatingActionButton: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TeamsTopBar(
                onBack = onBack,
                onOpenPreset = onOpenPreset,
                hasPresets = hasPresets,
                onManagePeople = onManagePeople,
                onSavePreset = onSavePreset
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        floatingActionButton = floatingActionButton,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = content
    )
}
