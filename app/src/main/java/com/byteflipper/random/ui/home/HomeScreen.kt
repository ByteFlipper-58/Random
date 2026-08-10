package com.byteflipper.random.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.ui.app.AppViewModel
import com.byteflipper.random.ui.home.components.CreateListDialog
import com.byteflipper.random.ui.home.components.HomeMenuBottomSheet
import com.byteflipper.random.ui.presets.PresetsExternalAction
import com.byteflipper.random.ui.presets.PresetsExternalActionType
import com.byteflipper.random.ui.presets.PresetsSelectionUiState
import com.byteflipper.random.ui.presets.PresetsViewModel
import kotlinx.coroutines.launch

enum class MenuItemType {
    NUMBERS,
    LIST,
    DICE,
    LOT,
    COIN,
    WHEEL,
    TEAMS;

    val iconRes: Int
        get() = when (this) {
            NUMBERS -> R.drawable.looks_one_24px
            LIST -> R.drawable.list_alt_24px
            DICE -> R.drawable.ifl_24px
            LOT -> R.drawable.gavel_24px
            COIN -> R.drawable.paid_24px
            WHEEL -> R.drawable.casino_24px
            TEAMS -> R.drawable.groups_24px
        }

    val titleRes: Int
        get() = when (this) {
            NUMBERS -> R.string.numbers
            LIST -> R.string.list
            DICE -> R.string.dice
            LOT -> R.string.lot
            COIN -> R.string.coin
            WHEEL -> R.string.wheel
            TEAMS -> R.string.teams
        }

    val supportsQuickAdd: Boolean
        get() = this == LIST
}

@Composable
fun HomeScreen(
    appViewModel: AppViewModel,
    onOpenNumbers: () -> Unit,
    onOpenList: () -> Unit,
    onOpenListPreset: (ListPreset) -> Unit,
    onOpenTeamPreset: (Long) -> Unit,
    onOpenDice: () -> Unit,
    onOpenLot: () -> Unit,
    onOpenCoin: () -> Unit,
    onOpenWheel: () -> Unit,
    onOpenTeams: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val presetsViewModel: PresetsViewModel = hiltViewModel()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val pendingSharedImport by appViewModel.pendingSharedImport.collectAsStateWithLifecycle()
    val sessionSelectedTab by viewModel.sessionSelectedTab.collectAsStateWithLifecycle()
    val presetsUiState by presetsViewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(
        initialPage = sessionSelectedTab.ordinal,
        pageCount = { HomeTab.entries.size }
    )
    val scope = rememberCoroutineScope()
    val selectedTab by remember(pagerState) {
        derivedStateOf { HomeTab.entries[pagerState.currentPage] }
    }
    val selectionProgress by remember(pagerState) {
        derivedStateOf {
            (
                pagerState.currentPage.toFloat() + pagerState.currentPageOffsetFraction
                ).coerceIn(0f, (HomeTab.entries.size - 1).toFloat())
        }
    }

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showPresetsSearch by rememberSaveable { mutableStateOf(false) }
    var showPresetsFabMenu by rememberSaveable { mutableStateOf(false) }
    var isPresetsFilterInteracting by remember { mutableStateOf(false) }
    var presetsSelectionState by remember { mutableStateOf(PresetsSelectionUiState()) }
    var pendingPresetsAction by remember { mutableStateOf<PresetsExternalAction?>(null) }
    var menuItems by rememberSaveable { mutableStateOf(MenuItemType.entries.toList()) }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in menuItems.indices || toIndex !in menuItems.indices) return
        val newItems = menuItems.toMutableList()
        val moved = newItems.removeAt(fromIndex)
        newItems.add(toIndex, moved)
        menuItems = newItems
    }

    fun postPresetsAction(type: PresetsExternalActionType) {
        pendingPresetsAction = PresetsExternalAction(type = type)
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab != HomeTab.Presets && showPresetsSearch) {
            showPresetsSearch = false
        }
        if (selectedTab != HomeTab.Presets) {
            showPresetsFabMenu = false
        }
    }

    LaunchedEffect(showPresetsSearch, presetsSelectionState.active) {
        if (showPresetsSearch || presetsSelectionState.active) {
            showPresetsFabMenu = false
        }
    }

    LaunchedEffect(pendingSharedImport?.id) {
        if (pendingSharedImport != null && pagerState.currentPage != HomeTab.Presets.ordinal) {
            pagerState.animateScrollToPage(HomeTab.Presets.ordinal)
            viewModel.updateSelectedTab(HomeTab.Presets)
        }
    }

    LaunchedEffect(sessionSelectedTab) {
        if (pagerState.currentPage != sessionSelectedTab.ordinal) {
            pagerState.scrollToPage(sessionSelectedTab.ordinal)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val currentTab = HomeTab.entries[pagerState.currentPage]
        if (currentTab != sessionSelectedTab) {
            viewModel.updateSelectedTab(currentTab)
        }
    }

    BackHandler(
        enabled = selectedTab == HomeTab.Presets && presetsSelectionState.active && !showPresetsSearch
    ) {
        postPresetsAction(PresetsExternalActionType.ExitSelection)
    }

    val searchTopBar = HomePresetsSearchBar(
        selectedTab = selectedTab,
        showPresetsSearch = showPresetsSearch,
        presetsUiState = presetsUiState,
        onFilterChange = presetsViewModel::updateFilter,
        onToggleSortOrder = presetsViewModel::toggleSortOrder,
        onFilterInteractionChanged = { isInteracting ->
            isPresetsFilterInteracting = isInteracting
        },
        onOpenPreset = onOpenListPreset,
        onDismiss = { showPresetsSearch = false }
    )

    val topBarOverride = HomePresetsSelectionTopBar(
        selectedTab = selectedTab,
        showPresetsSearch = showPresetsSearch,
        selectionState = presetsSelectionState,
        onAction = ::postPresetsAction
    )

    val floatingActionButton = HomePresetsFab(
        selectedTab = selectedTab,
        showPresetsSearch = showPresetsSearch,
        selectionState = presetsSelectionState,
        expanded = showPresetsFabMenu,
        hasPresets = presetsUiState.hasAnyPresets,
        onExpandedChange = { showPresetsFabMenu = it },
        onCreatePreset = {
            showPresetsFabMenu = false
            showCreateDialog = true
        },
        onAction = { type ->
            showPresetsFabMenu = false
            postPresetsAction(type)
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScaffold(
            selectionProgress = selectionProgress,
            selectedTab = selectedTab,
            onSelectTab = { tab ->
                scope.launch {
                    pagerState.animateScrollToPage(
                        page = tab.ordinal,
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            },
            onOpenMenu = { showMenu = true },
            onOpenSearch = { showPresetsSearch = true },
            searchTopBar = searchTopBar,
            topBarOverride = topBarOverride,
            floatingActionButton = floatingActionButton
        ) { inner ->
            HomePagerContent(
                pagerState = pagerState,
                innerPadding = inner,
                menuItems = menuItems,
                showPresetsSearch = showPresetsSearch,
                isPresetsFilterInteracting = isPresetsFilterInteracting,
                pendingSharedImport = pendingSharedImport,
                pendingPresetsAction = pendingPresetsAction,
                presetsViewModel = presetsViewModel,
                onMoveItem = ::moveItem,
                onOpenNumbers = onOpenNumbers,
                onOpenList = onOpenList,
                onAddList = { showCreateDialog = true },
                onOpenDice = onOpenDice,
                onOpenLot = onOpenLot,
                onOpenCoin = onOpenCoin,
                onOpenWheel = onOpenWheel,
                onOpenTeams = onOpenTeams,
                onOpenPreset = onOpenListPreset,
                onOpenTeamPreset = onOpenTeamPreset,
                onSharedImportConsumed = appViewModel::clearSharedImport,
                onExternalActionHandled = { actionId ->
                    if (pendingPresetsAction?.id == actionId) {
                        pendingPresetsAction = null
                    }
                },
                onSelectionStateChanged = { selectionState ->
                    presetsSelectionState = selectionState
                },
                onFilterInteractionChanged = { isInteracting ->
                    isPresetsFilterInteracting = isInteracting
                }
            )
        }
    }

    HomeMenuBottomSheet(
        visible = showMenu,
        onDismissRequest = { showMenu = false },
        onOpenAbout = onOpenAbout,
        onOpenSettings = onOpenSettings
    )

    CreateListDialog(
        showDialog = showCreateDialog,
        onDismiss = { showCreateDialog = false },
        presetCount = presets.size,
        onCreate = { name, items -> viewModel.onEvent(HomeUiEvent.CreatePreset(name, items)) },
        onPresetCreated = { showCreateDialog = false }
    )
}
