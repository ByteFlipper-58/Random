package com.byteflipper.random.ui.settings.graphics

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import com.byteflipper.random.ui.settings.SettingsGraphicsTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsGraphicsScaffold(
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = { SettingsGraphicsTopBar(onBack) },
        contentWindowInsets = WindowInsets.safeDrawing,
        content = content
    )
}
