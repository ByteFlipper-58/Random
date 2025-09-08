package com.byteflipper.random.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.ui.home.components.MenuCard
import com.byteflipper.random.ui.home.components.PresetCard
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    items: List<HomeItem>,
    onMoveItem: (Int, Int) -> Unit,
    onOpenNumbers: () -> Unit,
    onOpenList: () -> Unit,
    onAddList: () -> Unit,
    onOpenListById: (Long) -> Unit,
    onOpenDice: () -> Unit,
    onOpenLot: () -> Unit,
    onOpenCoin: () -> Unit,
    onRenamePreset: (ListPreset) -> Unit,
    onDeletePreset: (ListPreset) -> Unit
) {
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMoveItem(from.index, to.index)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp, 0.dp),
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(
            items = items,
            key = { item -> when (item) { is HomeItem.MenuItem -> "menu_${item.type}"; is HomeItem.PresetItem -> "preset_${item.preset.id}" } }
        ) { item ->
            ReorderableItem(
                state = reorderState,
                key = when (item) { is HomeItem.MenuItem -> "menu_${item.type}"; is HomeItem.PresetItem -> "preset_${item.preset.id}" }
            ) { isDragging ->
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 4.dp else 0.dp,
                    label = "drag-elevation"
                )

                Box() {
                    val dragModifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .longPressDraggableHandle()

                    when (item) {
                        is HomeItem.MenuItem -> {
                            val onAdd: (() -> Unit)? = when (item.type) {
                                MenuItemType.NUMBERS -> null
                                MenuItemType.LIST -> onAddList
                                else -> null
                            }

                            MenuCard(
                                icon = when (item.type) {
                                    MenuItemType.NUMBERS -> painterResource(id = R.drawable.looks_one_24px)
                                    MenuItemType.LIST -> painterResource(id = R.drawable.list_alt_24px)
                                    MenuItemType.DICE -> painterResource(id = R.drawable.ifl_24px)
                                    MenuItemType.LOT -> painterResource(id = R.drawable.gavel_24px)
                                    MenuItemType.COIN -> painterResource(id = R.drawable.paid_24px)
                                },
                                title = when (item.type) {
                                    MenuItemType.NUMBERS -> stringResource(R.string.numbers)
                                    MenuItemType.LIST -> stringResource(R.string.list)
                                    MenuItemType.DICE -> stringResource(R.string.dice)
                                    MenuItemType.LOT -> stringResource(R.string.lot)
                                    MenuItemType.COIN -> stringResource(R.string.coin)
                                },
                                onClick = when (item.type) {
                                    MenuItemType.NUMBERS -> onOpenNumbers
                                    MenuItemType.LIST -> onOpenList
                                    MenuItemType.DICE -> onOpenDice
                                    MenuItemType.LOT -> onOpenLot
                                    MenuItemType.COIN -> onOpenCoin
                                },
                                onAddClick = onAdd,
                                modifier = dragModifier
                            )
                        }

                        is HomeItem.PresetItem -> {
                            PresetCard(
                                preset = item.preset,
                                onPresetClick = { preset -> onOpenListById(preset.id) },
                                onRenameClick = { preset -> onRenamePreset(preset) },
                                onDeleteClick = { preset -> onDeletePreset(preset) },
                                modifier = dragModifier
                            )
                        }
                    }
                }
            }
        }
    }
}


