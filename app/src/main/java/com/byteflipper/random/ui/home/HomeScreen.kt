package com.byteflipper.random.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.data.settings.HapticsIntensity
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
import com.byteflipper.random.ui.home.components.HomeMenuBottomSheet
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
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()


    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }
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

    HomeScaffold(onOpenMenu = { showMenu = true }) { inner ->
        HomeContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            items = items,
            onMoveItem = { a, b -> moveItem(a, b) },
            onOpenNumbers = onOpenNumbers,
            onOpenList = onOpenList,
            onAddList = { showCreateDialog = true },
            onOpenListById = onOpenListById,
            onOpenDice = onOpenDice,
            onOpenLot = onOpenLot,
            onOpenCoin = onOpenCoin,
            onRenamePreset = { renameTarget = it },
            onDeletePreset = { viewModel.onEvent(HomeUiEvent.DeletePreset(it)) }
        )
    }

    HomeMenuBottomSheet(
        visible = showMenu,
        onDismissRequest = { showMenu = false },
        onOpenAbout = onOpenAbout,
        onOpenSettings = onOpenSettings
    )

    CreateListDialog(
        showDialog = showCreateDialog,
        onDismiss = {
            showCreateDialog = false
            createName = ""
        },
        presetCount = presets.size,
        onCreate = { name, items -> viewModel.onEvent(HomeUiEvent.CreatePreset(name, items)) },
        onPresetCreated = {
            showCreateDialog = false
            createName = ""
        }
    )

    if (renameTarget != null) {
        RenameListDialog(
            preset = renameTarget,
            onDismiss = { renameTarget = null },
            onRename = { preset, newName -> viewModel.onEvent(HomeUiEvent.RenamePreset(preset, newName)) },
            onPresetRenamed = { renameTarget = null }
        )
    }
}