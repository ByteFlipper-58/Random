package com.byteflipper.random.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.ui.home.components.CreateListDialog
import com.byteflipper.random.ui.home.components.HomeMenuBottomSheet
import com.byteflipper.random.ui.presets.PresetsContent
import com.byteflipper.random.ui.presets.PresetsSearchTopBar
import com.byteflipper.random.ui.presets.PresetsViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

enum class MenuItemType {
    NUMBERS,
    LIST,
    DICE,
    LOT,
    COIN,
    WHEEL;

    val iconRes: Int
        get() = when (this) {
            NUMBERS -> R.drawable.looks_one_24px
            LIST -> R.drawable.list_alt_24px
            DICE -> R.drawable.ifl_24px
            LOT -> R.drawable.gavel_24px
            COIN -> R.drawable.paid_24px
            WHEEL -> R.drawable.casino_24px
        }

    val titleRes: Int
        get() = when (this) {
            NUMBERS -> R.string.numbers
            LIST -> R.string.list
            DICE -> R.string.dice
            LOT -> R.string.lot
            COIN -> R.string.coin
            WHEEL -> R.string.wheel
        }

    val supportsQuickAdd: Boolean
        get() = this == LIST
}

@Composable
fun HomeScreen(
    onOpenNumbers: () -> Unit,
    onOpenList: () -> Unit,
    onOpenListPreset: (ListPreset) -> Unit,
    onOpenDice: () -> Unit,
    onOpenLot: () -> Unit,
    onOpenCoin: () -> Unit,
    onOpenWheel: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val presetsViewModel: PresetsViewModel = hiltViewModel()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
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
    var isPresetsFilterInteracting by remember { mutableStateOf(false) }
    var menuItems by rememberSaveable { mutableStateOf(MenuItemType.entries.toList()) }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in menuItems.indices || toIndex !in menuItems.indices) return
        val newItems = menuItems.toMutableList()
        val moved = newItems.removeAt(fromIndex)
        newItems.add(toIndex, moved)
        menuItems = newItems
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab != HomeTab.Presets && showPresetsSearch) {
            showPresetsSearch = false
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
            searchTopBar = if (selectedTab == HomeTab.Presets && showPresetsSearch) {
                {
                    PresetsSearchTopBar(
                        uiState = presetsUiState,
                        onFilterChange = presetsViewModel::updateFilter,
                        onToggleSortOrder = presetsViewModel::toggleSortOrder,
                        onFilterInteractionChanged = { isInteracting ->
                            isPresetsFilterInteracting = isInteracting
                        },
                        onOpenPreset = onOpenListPreset,
                        onDismiss = {
                            showPresetsSearch = false
                        }
                    )
                }
            } else {
                null
            }
        ) { inner ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
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
                        onMoveItem = ::moveItem,
                        onOpenNumbers = onOpenNumbers,
                        onOpenList = onOpenList,
                        onAddList = { showCreateDialog = true },
                        onOpenDice = onOpenDice,
                        onOpenLot = onOpenLot,
                        onOpenCoin = onOpenCoin,
                        onOpenWheel = onOpenWheel
                    )

                    HomeTab.Presets -> PresetsContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = pageScale
                                scaleY = pageScale
                                alpha = pageAlpha
                            },
                        onOpenPreset = onOpenListPreset,
                        onCreatePreset = { showCreateDialog = true },
                        viewModel = presetsViewModel,
                        onFilterInteractionChanged = { isInteracting ->
                            isPresetsFilterInteracting = isInteracting
                        }
                    )
                }
            }
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
