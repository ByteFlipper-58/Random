package com.byteflipper.random.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.ui.home.components.MenuCard
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    menuItems: List<MenuItemType>,
    onMoveItem: (Int, Int) -> Unit,
    onOpenNumbers: () -> Unit,
    onOpenList: () -> Unit,
    onAddList: () -> Unit,
    onOpenDice: () -> Unit,
    onOpenLot: () -> Unit,
    onOpenCoin: () -> Unit,
    onOpenWheel: () -> Unit,
    onOpenTeams: () -> Unit
) {
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMoveItem(from.index, to.index)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        state = lazyListState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(
            items = menuItems,
            key = { type -> "menu_$type" }
        ) { type ->
            ReorderableItem(
                state = reorderState,
                key = "menu_$type"
            ) {
                Box {
                    val dragModifier = with(this) {
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .longPressDraggableHandle()
                    }
                    val onAdd = onAddList.takeIf { type.supportsQuickAdd }
                    val onClick = when (type) {
                        MenuItemType.NUMBERS -> onOpenNumbers
                        MenuItemType.LIST -> onOpenList
                        MenuItemType.DICE -> onOpenDice
                        MenuItemType.LOT -> onOpenLot
                        MenuItemType.COIN -> onOpenCoin
                        MenuItemType.WHEEL -> onOpenWheel
                        MenuItemType.TEAMS -> onOpenTeams
                    }

                    MenuCard(
                        icon = painterResource(id = type.iconRes),
                        title = stringResource(type.titleRes),
                        onClick = onClick,
                        onAddClick = onAdd,
                        modifier = dragModifier
                    )
                }
            }
        }
    }
}
