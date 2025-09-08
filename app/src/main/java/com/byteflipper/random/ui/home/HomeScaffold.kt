package com.byteflipper.random.ui.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScaffold(
    onOpenAbout: () -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        topBar = { HomeTopBar(onOpenAbout, onOpenSettings) },
        contentWindowInsets = WindowInsets.systemBars,
        content = content
    )
}


