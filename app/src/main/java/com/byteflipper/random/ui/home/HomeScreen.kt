package com.byteflipper.random.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.ui.home.components.CreateListDialog
import com.byteflipper.random.ui.home.components.MenuCard
import com.byteflipper.random.ui.home.components.PresetCard
import com.byteflipper.random.ui.home.components.RenameListDialog
 
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

// Типы элементов для drag & drop
sealed class HomeItem {
    data class MenuItem(val type: MenuItemType) : HomeItem()
    data class PresetItem(val preset: ListPreset) : HomeItem()
}

enum class MenuItemType {
    NUMBERS, LIST, DICE, LOT, COIN
}

private fun keyFor(item: HomeItem): String = when (item) {
    is HomeItem.MenuItem -> "menu_${item.type}"
    is HomeItem.PresetItem -> "preset_${item.preset.id}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenNumbers: () -> Unit,
    onOpenList: () -> Unit,
    onOpenListById: (Long) -> Unit,
    onOpenDice: () -> Unit,
    onOpenLot: () -> Unit,
    onOpenCoin: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onAddNumbersPreset: () -> Unit,
    onAddListPreset: () -> Unit, // оставлен для совместимости
) {
 
    val viewModel: HomeViewModel = hiltViewModel()
    val presets by viewModel.presets.collectAsStateWithLifecycle()


    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var createName by rememberSaveable { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<ListPreset?>(null) }

    // Состояние для элементов
    var items by remember { mutableStateOf<List<HomeItem>>(emptyList()) }

    // Формируем список (меню + пресеты) при изменении пресетов
    LaunchedEffect(presets) {
        val menuItems = listOf(
            HomeItem.MenuItem(MenuItemType.NUMBERS),
            HomeItem.MenuItem(MenuItemType.LIST),
            HomeItem.MenuItem(MenuItemType.DICE),
            HomeItem.MenuItem(MenuItemType.LOT),
            HomeItem.MenuItem(MenuItemType.COIN)
        )
        val presetItems = presets.map { HomeItem.PresetItem(it) }
        items = menuItems + presetItems
    }

    // Переупорядочивание
    fun moveItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in items.indices || toIndex !in items.indices) return
        val newItems = items.toMutableList()
        val moved = newItems.removeAt(fromIndex)
        newItems.add(toIndex, moved)
        items = newItems
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.random),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onOpenAbout) {
                        Icon(
                            painterResource(id = R.drawable.info_24px),
                            contentDescription = stringResource(R.string.about_app),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            painterResource(id = R.drawable.settings_24px),
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { inner ->
        val haptic = LocalHapticFeedback.current
        val lazyListState = rememberLazyListState()

        val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
            moveItem(from.index, to.index)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp, 0.dp),
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(
                items = items,
                key = { item -> keyFor(item) }
            ) { item ->
                ReorderableItem(
                    state = reorderState,
                    key = keyFor(item)
                ) { isDragging ->
                    val elevation by animateDpAsState(
                        targetValue = if (isDragging) 4.dp else 0.dp,
                        label = "drag-elevation"
                    )

                    Box() {
                        val dragModifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .longPressDraggableHandle(
                                onDragStarted = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )

                        when (item) {
                            is HomeItem.MenuItem -> {
                                val onAdd: (() -> Unit)? = when (item.type) {
                                    MenuItemType.NUMBERS -> null
                                    MenuItemType.LIST -> { { showCreateDialog = true } }
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
                                    onRenameClick = { preset -> renameTarget = preset },
                                    onDeleteClick = { preset -> viewModel.deletePreset(preset) },
                                    modifier = dragModifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    CreateListDialog(
        showDialog = showCreateDialog,
        onDismiss = {
            showCreateDialog = false
            createName = ""
        },
        presetCount = presets.size,
        onCreate = { name, items -> viewModel.createPreset(name, items) },
        onPresetCreated = {
            showCreateDialog = false
            createName = ""
        }
    )

    if (renameTarget != null) {
        RenameListDialog(
            preset = renameTarget,
            onDismiss = { renameTarget = null },
            onRename = { preset, newName -> viewModel.renamePreset(preset, newName) },
            onPresetRenamed = { renameTarget = null }
        )
    }
}