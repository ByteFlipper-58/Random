package com.byteflipper.random.ui.about

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScaffold(
    onBack: () -> Unit,
    content: @Composable (innerPadding: androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        topBar = { AboutTopBar(onBack) },
        contentWindowInsets = WindowInsets.systemBars,
        content = content
    )
}


