package com.byteflipper.random.ui.wheel.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal class WheelPresetSelectionListState(
    val listState: LazyListState
) {
    var isBottomBarVisible by mutableStateOf(true)
    var previousPosition by mutableLongStateOf(0L)
}

@Composable
internal fun rememberWheelPresetSelectionListState(): WheelPresetSelectionListState {
    val listState = rememberLazyListState()
    val state = remember(listState) { WheelPresetSelectionListState(listState) }

    LaunchedEffect(state.listState) {
        snapshotFlow {
            state.listState.firstVisibleItemIndex.toLong() * 100_000L +
                state.listState.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collect { currentPosition ->
                when {
                    currentPosition <= 0L -> state.isBottomBarVisible = true
                    currentPosition > state.previousPosition -> state.isBottomBarVisible = false
                    currentPosition < state.previousPosition -> state.isBottomBarVisible = true
                }
                state.previousPosition = currentPosition
            }
    }

    return state
}
