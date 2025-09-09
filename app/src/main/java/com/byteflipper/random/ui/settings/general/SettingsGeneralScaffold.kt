package com.byteflipper.random.ui.settings.general

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import com.byteflipper.random.ui.settings.SettingsGeneralTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsGeneralScaffold(
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = { SettingsGeneralTopBar(onBack) },
        contentWindowInsets = WindowInsets.systemBars,
        content = content
    )
}


