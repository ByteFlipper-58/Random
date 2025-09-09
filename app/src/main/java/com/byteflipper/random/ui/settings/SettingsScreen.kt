package com.byteflipper.random.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenGeneral: () -> Unit,
    onOpenAppearance: () -> Unit
) {
    SettingsScaffold(onBack) { inner ->
        SettingsContent(
            modifier = Modifier.padding(inner),
            onOpenGeneral = onOpenGeneral,
            onOpenAppearance = onOpenAppearance
        )
    }
}

