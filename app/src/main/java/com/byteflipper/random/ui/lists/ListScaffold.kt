package com.byteflipper.random.ui.lists

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.byteflipper.random.data.preset.ListPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScaffold(
    onBack: () -> Unit,
    title: String,
    presets: List<ListPreset> = emptyList(),
    onAddItems: (List<String>) -> Unit = {},
    onShowSave: (() -> Unit)? = null,
    onShowSaveResults: (() -> Unit)? = null,
    onShowRename: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState,
    floatingActionButton: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            ListTopBar(
                onBack = onBack,
                title = title,
                presets = presets,
                onAddItems = onAddItems,
                onShowSave = onShowSave,
                onShowSaveResults = onShowSaveResults,
                onShowRename = onShowRename
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        floatingActionButton = floatingActionButton,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = content
    )
}


