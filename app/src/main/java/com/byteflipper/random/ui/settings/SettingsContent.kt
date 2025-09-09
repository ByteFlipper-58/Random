package com.byteflipper.random.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.ui.settings.components.SettingsCategoryCard

@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    onOpenGeneral: () -> Unit,
    onOpenAppearance: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SettingsCategoryCard(
            title = stringResource(R.string.general),
            description = stringResource(R.string.settings_general_subtitle),
            iconRes = R.drawable.settings_24px,
            onClick = onOpenGeneral,
        )

        SettingsCategoryCard(
            title = stringResource(R.string.appearance),
            description = stringResource(R.string.settings_appearance_subtitle),
            iconRes = R.drawable.palette_24px,
            onClick = onOpenAppearance,
        )

        Spacer(Modifier.height(4.dp))
    }
}


