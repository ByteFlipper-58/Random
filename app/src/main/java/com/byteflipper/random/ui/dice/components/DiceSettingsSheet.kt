package com.byteflipper.random.ui.dice.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.ui.components.ConfigDivider
import com.byteflipper.random.ui.components.ConfigHeader
import com.byteflipper.random.ui.components.ConfigSection

/**
 * The dice generator's own settings: which dice a roll here draws, and what starts one.
 *
 * How much a frame of the tray's simulation may cost is not among them — that is the device's
 * business rather than this roll's, and it lives in the app's graphics settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceSettingsSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    threeDimensional: Boolean,
    onThreeDimensionalChange: (Boolean) -> Unit,
    shakeToRoll: Boolean,
    onShakeToRollChange: (Boolean) -> Unit
) {
    if (!visible) return

    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(modifier = Modifier.size(width = 32.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            ConfigHeader()
            ConfigDivider()

            ConfigSection(
                icon = painterResource(id = R.drawable.casino_24px),
                title = stringResource(R.string.dice_3d),
                description = stringResource(R.string.dice_3d_desc),
                action = {
                    Switch(checked = threeDimensional, onCheckedChange = onThreeDimensionalChange)
                }
            )

            ConfigDivider()

            // The app-wide shake switch, surfaced here because this is a screen it acts on.
            ConfigSection(
                icon = painterResource(id = R.drawable.mobile_vibrate_24px),
                title = stringResource(R.string.shake_to_generate),
                description = stringResource(R.string.shake_to_generate_description),
                action = {
                    Switch(checked = shakeToRoll, onCheckedChange = onShakeToRollChange)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
