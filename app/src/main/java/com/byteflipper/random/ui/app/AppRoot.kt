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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.data.settings.ThemeMode
import com.byteflipper.random.navigation.NavTransitions
import com.byteflipper.random.ui.about.AboutScreen
import com.byteflipper.random.ui.coin.CoinScreen
import com.byteflipper.random.ui.dice.DiceScreen
import com.byteflipper.random.ui.home.HomeScreen
import com.byteflipper.random.ui.lists.ListScreen
import com.byteflipper.random.ui.settings.appearance.SettingsAppearanceScreen
import com.byteflipper.random.ui.settings.general.SettingsGeneralScreen
import com.byteflipper.random.ui.settings.SettingsScreen
import com.byteflipper.random.ui.theme.RandomTheme
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.ui.components.SystemHapticsManager
import com.byteflipper.random.ui.numbers.NumbersScreen
import com.byteflipper.random.ui.lot.LotScreen
import com.byteflipper.random.ui.setup.SetupScreen

object AppRoutes {
    const val Setup: String = "setup"
    const val Home: String = "home"
    const val Numbers: String = "numbers"
    const val List: String = "list"
    const val ListWithId: String = "list/{id}"
    const val Dice: String = "dice"
    const val Lot: String = "lot"
    const val Coin: String = "coin"
    const val Settings: String = "settings"
    const val SettingsGeneral: String = "settings_general"
    const val SettingsAppearance: String = "settings_appearance"
    const val About: String = "about"
}

@Composable
fun AppRoot() {
    val viewModel: AppViewModel = hiltViewModel()
    val context = LocalContext.current

    val initialSettings: Settings? by viewModel.initialSettings.collectAsStateWithLifecycle()
    val settings: Settings by viewModel.settingsFlow.collectAsStateWithLifecycle(
        initialValue = initialSettings ?: Settings()
    )

    LaunchedEffect(settings.appLanguage) {
        val tag = settings.appLanguage.localeTag
        val desiredLocales = if (tag == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != desiredLocales.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(desiredLocales)
        }
    }

    val darkTheme = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    RandomTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColors) {
        val hapticsManager = remember { SystemHapticsManager(context) }
        androidx.compose.runtime.CompositionLocalProvider(LocalHapticsManager provides hapticsManager) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val navController = rememberNavController()
                val startDestination = if (settings.setupCompleted) AppRoutes.Home else AppRoutes.Setup

                NavHost(navController = navController, startDestination = startDestination) {
                    composable(AppRoutes.Setup) {
                        SetupScreen(onSetupComplete = {
                            navController.navigate(AppRoutes.Home) {
                                popUpTo(AppRoutes.Setup) { inclusive = true }
                            }
                        })
                    }
                    composable(AppRoutes.Home) {
                        HomeScreen(
                            onOpenNumbers = { navController.navigate(AppRoutes.Numbers) },
                            onOpenList = { navController.navigate(AppRoutes.List) },
                            onOpenListById = { id -> navController.navigate("list/$id") },
                            onOpenDice = { navController.navigate(AppRoutes.Dice) },
                            onOpenLot = { navController.navigate(AppRoutes.Lot) },
                            onOpenCoin = { navController.navigate(AppRoutes.Coin) },
                            onOpenSettings = { navController.navigate(AppRoutes.Settings) },
                            onOpenAbout = { navController.navigate(AppRoutes.About) },
                            onAddNumbersPreset = { },
                            onAddListPreset = { navController.navigate("add_list_preset") }
                        )
                    }
                    composable(
                        route = AppRoutes.Numbers,
                        enterTransition = NavTransitions.enter,
                        exitTransition = NavTransitions.exit,
                        popEnterTransition = NavTransitions.popEnter,
                        popExitTransition = NavTransitions.popExit
                    ) { NumbersScreen(onBack = { navController.popBackStack() }) }

                    composable(
                        route = AppRoutes.Lot,
                        enterTransition = NavTransitions.enter,
                        exitTransition = NavTransitions.exit,
                        popEnterTransition = NavTransitions.popEnter,
                        popExitTransition = NavTransitions.popExit
                    ) { LotScreen(onBack = { navController.popBackStack() }) }

                    composable(
                        route = AppRoutes.Dice,
                        enterTransition = NavTransitions.enter,
                        exitTransition = NavTransitions.exit,
                        popEnterTransition = NavTransitions.popEnter,
                        popExitTransition = NavTransitions.popExit
                    ) { DiceScreen(onBack = { navController.popBackStack() }) }

                    composable(
                        route = AppRoutes.Coin,
                        enterTransition = NavTransitions.enter,
                        exitTransition = NavTransitions.exit,
                        popEnterTransition = NavTransitions.popEnter,
                        popExitTransition = NavTransitions.popExit
                    ) { CoinScreen(onBack = { navController.popBackStack() }) }

                    composable(
                        route = AppRoutes.Settings,
                        enterTransition = NavTransitions.enter,
                        exitTransition = NavTransitions.exit,
                        popEnterTransition = NavTransitions.popEnter,
                        popExitTransition = NavTransitions.popExit
                    ) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenGeneral = { navController.navigate(AppRoutes.SettingsGeneral) },
                            onOpenAppearance = { navController.navigate(AppRoutes.SettingsAppearance) }
                        )
                    }

                    composable(
                        route = AppRoutes.SettingsGeneral,
                        enterTransition = NavTransitions.enter,
                        exitTransition = NavTransitions.exit,
                        popEnterTransition = NavTransitions.popEnter,
                        popExitTransition = NavTransitions.popExit
                    ) { SettingsGeneralScreen(onBack = { navController.popBackStack() }) }

                    composable(
                        route = AppRoutes.SettingsAppearance,
                        enterTransition = NavTransitions.enter,
                        exitTransition = NavTransitions.exit,
                        popEnterTransition = NavTransitions.popEnter,
                        popExitTransition = NavTransitions.popExit
                    ) { SettingsAppearanceScreen(onBack = { navController.popBackStack() }) }

                    composable(
                        route = AppRoutes.About,
                        enterTransition = NavTransitions.enter,
                        exitTransition = NavTransitions.exit,
                        popEnterTransition = NavTransitions.popEnter,
                        popExitTransition = NavTransitions.popExit
                    ) { AboutScreen(onBack = { navController.popBackStack() }) }

                    composable(
                        route = AppRoutes.List,
                        enterTransition = NavTransitions.enter,
                        exitTransition = NavTransitions.exit,
                        popEnterTransition = NavTransitions.popEnter,
                        popExitTransition = NavTransitions.popExit
                    ) {
                        ListScreen(
                            onBack = { navController.popBackStack() },
                            onOpenListById = { id ->
                                navController.navigate("list/$id") {
                                    popUpTo(AppRoutes.Home) { inclusive = false }
                                }
                            }
                        )
                    }

                    composable(
                        route = AppRoutes.ListWithId,
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                        enterTransition = NavTransitions.enter,
                        exitTransition = NavTransitions.exit,
                        popEnterTransition = NavTransitions.popEnter,
                        popExitTransition = NavTransitions.popExit
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getLong("id")
                        ListScreen(
                            onBack = { navController.popBackStack() },
                            presetId = id,
                            onOpenListById = { newId -> navController.navigate("list/$newId") }
                        )
                    }
                }
            }
        }
    }
}


