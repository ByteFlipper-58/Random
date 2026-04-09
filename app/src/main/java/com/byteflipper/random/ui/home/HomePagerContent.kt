package com.byteflipper.random.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.ui.app.PendingSharedImport
import com.byteflipper.random.ui.presets.PresetsExternalAction
import com.byteflipper.random.ui.presets.PresetsSelectionUiState
import com.byteflipper.random.ui.presets.PresetsViewModel
import kotlin.math.absoluteValue

@Composable
internal fun HomePagerContent(
    pagerState: PagerState,
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    menuItems: List<MenuItemType>,
    showPresetsSearch: Boolean,
    isPresetsFilterInteracting: Boolean,
    pendingSharedImport: PendingSharedImport?,
    pendingPresetsAction: PresetsExternalAction?,
    presetsViewModel: PresetsViewModel,
    onMoveItem: (Int, Int) -> Unit,
    onOpenNumbers: () -> Unit,
    onOpenList: () -> Unit,
    onAddList: () -> Unit,
    onOpenDice: () -> Unit,
    onOpenLot: () -> Unit,
    onOpenCoin: () -> Unit,
    onOpenWheel: () -> Unit,
    onOpenPreset: (ListPreset) -> Unit,
    onSharedImportConsumed: (Long) -> Unit,
    onExternalActionHandled: (Long) -> Unit,
    onSelectionStateChanged: (PresetsSelectionUiState) -> Unit,
    onFilterInteractionChanged: (Boolean) -> Unit
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        userScrollEnabled = !isPresetsFilterInteracting && !showPresetsSearch,
        beyondViewportPageCount = 1
    ) { page ->
        val pageOffset = (
            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            ).absoluteValue.coerceIn(0f, 1f)
        val motionProgress = FastOutSlowInEasing.transform(1f - pageOffset)
        val pageScale = lerp(0.985f, 1f, motionProgress)
        val pageAlpha = lerp(0.9f, 1f, motionProgress)

        when (HomeTab.entries[page]) {
            HomeTab.Tools -> HomeContent(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pageScale
                        scaleY = pageScale
                        alpha = pageAlpha
                    },
                menuItems = menuItems,
                onMoveItem = onMoveItem,
                onOpenNumbers = onOpenNumbers,
                onOpenList = onOpenList,
                onAddList = onAddList,
                onOpenDice = onOpenDice,
                onOpenLot = onOpenLot,
                onOpenCoin = onOpenCoin,
                onOpenWheel = onOpenWheel
            )

            HomeTab.Presets -> com.byteflipper.random.ui.presets.PresetsContent(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pageScale
                        scaleY = pageScale
                        alpha = pageAlpha
                    },
                onOpenPreset = onOpenPreset,
                onCreatePreset = onAddList,
                pendingSharedImport = pendingSharedImport,
                onSharedImportConsumed = onSharedImportConsumed,
                externalAction = pendingPresetsAction,
                onExternalActionHandled = onExternalActionHandled,
                onSelectionStateChanged = onSelectionStateChanged,
                viewModel = presetsViewModel,
                onFilterInteractionChanged = onFilterInteractionChanged
            )
        }
    }
}
