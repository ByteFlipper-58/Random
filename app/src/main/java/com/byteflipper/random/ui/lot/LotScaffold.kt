package com.byteflipper.random.ui.lot

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotScaffold(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    floatingActionButton: @Composable () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        topBar = { LotTopBar(onBack) },
        contentWindowInsets = WindowInsets.systemBars,
        floatingActionButton = floatingActionButton,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = content
    )
}


