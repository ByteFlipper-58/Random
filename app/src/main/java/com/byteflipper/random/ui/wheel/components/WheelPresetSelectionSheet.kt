package com.byteflipper.random.ui.wheel.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.settings.HapticsIntensity
import com.byteflipper.random.ui.components.LocalHapticsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelPresetSelectionSheet(
    preset: ListPreset,
    selectedIndices: Set<Int>,
    itemLimit: Int,
    onDismiss: () -> Unit,
    onSelectionChange: (Int) -> Unit,
    hapticsEnabled: Boolean,
    hapticsIntensity: HapticsIntensity,
    onConfirm: () -> Unit
) {
    val listState = rememberWheelPresetSelectionListState()
    val hapticsManager = LocalHapticsManager.current
    val selectedCount = selectedIndices.size
    val canConfirm = selectedCount in 2..itemLimit
    val bottomBarOffset by animateDpAsState(
        targetValue = if (listState.isBottomBarVisible) 0.dp else 164.dp,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 600f),
        label = "wheel_preset_bottom_bar_offset"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxHeight(),
        sheetGesturesEnabled = false,
        contentWindowInsets = {
            WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = WheelSelectionSheetShape,
        dragHandle = { WheelSheetDragHandle() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            WheelPresetSelectionBody(
                preset = preset,
                selectedIndices = selectedIndices,
                itemLimit = itemLimit,
                selectedCount = selectedCount,
                listState = listState.listState,
                onSelectionChange = { index ->
                    val willSelect = index !in selectedIndices && selectedCount < itemLimit
                    onSelectionChange(index)
                    if (willSelect && selectedCount + 1 == itemLimit && hapticsEnabled) {
                        hapticsManager?.performPress(hapticsIntensity)
                    }
                }
            )

            WheelPresetSelectionBottomBar(
                canConfirm = canConfirm,
                bottomBarOffset = bottomBarOffset,
                onDismiss = onDismiss,
                onConfirm = {
                    if (hapticsEnabled) {
                        hapticsManager?.performPress(hapticsIntensity)
                    }
                    onConfirm()
                }
            )
        }
    }
}
