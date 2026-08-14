package com.byteflipper.random.ui.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.byteflipper.random.data.settings.Settings as AppSettings
import com.byteflipper.random.data.settings.ThemeMode
import com.byteflipper.random.navigation.About
import com.byteflipper.random.navigation.Coin
import com.byteflipper.random.navigation.Dice
import com.byteflipper.random.navigation.Finger
import com.byteflipper.random.navigation.Home
import com.byteflipper.random.navigation.ListEditor
import com.byteflipper.random.navigation.Lot
import com.byteflipper.random.navigation.NavTransitions
import com.byteflipper.random.navigation.Numbers
import com.byteflipper.random.navigation.People
import com.byteflipper.random.navigation.PeoplePicker
import com.byteflipper.random.navigation.Settings
import com.byteflipper.random.navigation.SettingsAppearance
import com.byteflipper.random.navigation.SettingsGeneral
import com.byteflipper.random.navigation.Setup
import com.byteflipper.random.navigation.Teams
import com.byteflipper.random.navigation.Wheel
import com.byteflipper.random.navigation.rememberNavigationState
import com.byteflipper.random.navigation.rememberNavigator
import com.byteflipper.random.ui.about.AboutScreen
import com.byteflipper.random.ui.coin.CoinScreen
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.ui.components.SystemHapticsManager
import com.byteflipper.random.ui.dice.DiceScreen
import com.byteflipper.random.ui.finger.FingerScreen
import com.byteflipper.random.ui.home.HomeScreen
import com.byteflipper.random.ui.lists.ListScreen
import com.byteflipper.random.ui.lists.ListViewModel
import com.byteflipper.random.ui.lot.LotScreen
import com.byteflipper.random.ui.numbers.NumbersScreen
import com.byteflipper.random.ui.people.PeopleScreen
import com.byteflipper.random.ui.settings.SettingsScreen
import com.byteflipper.random.ui.settings.appearance.SettingsAppearanceScreen
import com.byteflipper.random.ui.settings.general.SettingsGeneralScreen
import com.byteflipper.random.ui.setup.SetupScreen
import com.byteflipper.random.ui.teams.PeoplePickerScreen
import com.byteflipper.random.ui.teams.TeamsScreen
import com.byteflipper.random.ui.teams.TeamsViewModel
import com.byteflipper.random.ui.theme.RandomTheme
import com.byteflipper.random.ui.theme.model.Theme
import com.byteflipper.random.ui.wheel.WheelScreen

@Composable
fun AppRoot() {
    val appViewModel: AppViewModel = hiltViewModel()
    val context = LocalContext.current
    val initialSettings: AppSettings? by appViewModel.initialSettings.collectAsStateWithLifecycle()
    val settings: AppSettings by appViewModel.settingsFlow.collectAsStateWithLifecycle(
        initialValue = initialSettings ?: AppSettings()
    )

    if (initialSettings == null) return

    val darkTheme = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val navigationState = rememberNavigationState(
        initialKey = if (settings.setupCompleted) Home else Setup
    )
    val navigator = rememberNavigator(navigationState)

    RandomTheme(
        theme = if (settings.dynamicColors) Theme.DYNAMIC else Theme.BLUE,
        darkTheme = darkTheme
    ) {
        val hapticsManager = remember { SystemHapticsManager(context) }
        androidx.compose.runtime.CompositionLocalProvider(LocalHapticsManager provides hapticsManager) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                NavDisplay(
                    backStack = navigationState.backStack,
                    onBack = { navigator.goBack() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    transitionSpec = { NavTransitions.forward() },
                    popTransitionSpec = { NavTransitions.backward() },
                    predictivePopTransitionSpec = { NavTransitions.backward() },
                    entryProvider = entryProvider {
                        entry<Setup> {
                            SetupScreen(onSetupComplete = { navigator.replaceRoot(Home) })
                        }
                        entry<Home> {
                            HomeScreen(
                                appViewModel = appViewModel,
                                onOpenNumbers = { navigator.navigate(Numbers) },
                                onOpenList = { navigator.navigate(ListEditor()) },
                                onOpenListPreset = { preset -> navigator.navigate(ListEditor(preset.id)) },
                                onOpenTeamPreset = { id -> navigator.navigate(Teams(id)) },
                                onOpenDice = { navigator.navigate(Dice) },
                                onOpenLot = { navigator.navigate(Lot) },
                                onOpenCoin = { navigator.navigate(Coin) },
                                onOpenWheel = { navigator.navigate(Wheel) },
                                onOpenTeams = { navigator.navigate(Teams()) },
                                onOpenFinger = { navigator.navigate(Finger) },
                                onOpenSettings = { navigator.navigate(Settings) },
                                onOpenAbout = { navigator.navigate(About) }
                            )
                        }
                        entry<Numbers> { NumbersScreen(onBack = { navigator.goBack() }) }
                        entry<Lot> { LotScreen(onBack = { navigator.goBack() }) }
                        entry<Dice> { DiceScreen(onBack = { navigator.goBack() }) }
                        entry<Coin> { CoinScreen(onBack = { navigator.goBack() }) }
                        entry<Wheel> { WheelScreen(onBack = { navigator.goBack() }) }
                        entry<Finger> { FingerScreen(onBack = { navigator.goBack() }) }
                        entry<Teams> { key ->
                            val teamsViewModel = hiltViewModel<TeamsViewModel, TeamsViewModel.Factory>(
                                creationCallback = { factory -> factory.create(key.id) }
                            )
                            val pendingSelection = navigator.pendingTeamSelection(key)
                            LaunchedEffect(key, pendingSelection) {
                                pendingSelection?.let {
                                    teamsViewModel.setPersonSelection(it)
                                    navigator.consumeTeamSelection(key)
                                }
                            }
                            TeamsScreen(
                                viewModel = teamsViewModel,
                                onBack = { navigator.goBack() },
                                onManagePeople = { navigator.navigate(People) },
                                onPickMembers = {
                                    navigator.navigate(
                                        PeoplePicker(
                                            parent = key,
                                            selectedMemberIds = teamsViewModel.uiState.value.editor.selectedMemberIds
                                        )
                                    )
                                }
                            )
                        }
                        entry<PeoplePicker> { key ->
                            PeoplePickerScreen(
                                selectedMemberIds = key.selectedMemberIds,
                                onSelectionChanged = { selectedIds ->
                                    navigator.updatePeoplePickerSelection(key, selectedIds)
                                },
                                onBack = { navigator.goBack() }
                            )
                        }
                        entry<People> { PeopleScreen(onBack = { navigator.goBack() }) }
                        entry<Settings> {
                            SettingsScreen(
                                onBack = { navigator.goBack() },
                                onOpenGeneral = { navigator.navigate(SettingsGeneral) },
                                onOpenAppearance = { navigator.navigate(SettingsAppearance) }
                            )
                        }
                        entry<SettingsGeneral> { SettingsGeneralScreen(onBack = { navigator.goBack() }) }
                        entry<SettingsAppearance> { SettingsAppearanceScreen(onBack = { navigator.goBack() }) }
                        entry<About> { AboutScreen(onBack = { navigator.goBack() }) }
                        entry<ListEditor> { key ->
                            val listViewModel = hiltViewModel<ListViewModel, ListViewModel.Factory>(
                                creationCallback = { factory -> factory.create(key.id) }
                            )
                            ListScreen(
                                viewModel = listViewModel,
                                onBack = { navigator.goBack() },
                                presetId = key.id,
                                onOpenListById = navigator::openSavedList
                            )
                        }
                    }
                )
            }
        }
    }
}
