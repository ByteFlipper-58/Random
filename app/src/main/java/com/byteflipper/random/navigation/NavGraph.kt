package com.byteflipper.random.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.byteflipper.random.ui.home.HomeScreen
import com.byteflipper.random.ui.numbers.NumbersScreen
import com.byteflipper.random.ui.lists.ListScreen
import com.byteflipper.random.ui.coin.CoinScreen
import com.byteflipper.random.ui.lot.LotScreen
import com.byteflipper.random.ui.dice.DiceScreen
import com.byteflipper.random.ui.settings.SettingsScreen
import com.byteflipper.random.ui.settings.general.SettingsGeneralScreen
import com.byteflipper.random.ui.settings.appearance.SettingsAppearanceScreen
import com.byteflipper.random.ui.about.AboutScreen
import com.byteflipper.random.ui.setup.SetupScreen

sealed class Route(val route: String) {
    data object Setup : Route("setup")
    data object Home : Route("home")
    data object Numbers : Route("numbers")
    data object List : Route("list")
    data object ListWithId : Route("list/{id}")
    data object AddListPreset : Route("add_list_preset")
    data object Dice : Route("dice")
    data object Lot : Route("lot")
    data object Coin : Route("coin")
    data object Settings : Route("settings")
    data object SettingsGeneral : Route("settings_general")
    data object SettingsAppearance : Route("settings_appearance")
    data object About : Route("about")
}

@Composable
fun AppNavGraph(navController: NavHostController, startDestination: String = Route.Home.route) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Route.Setup.route) {
            SetupScreen(onSetupComplete = {
                navController.navigate(Route.Home.route) {
                    popUpTo(Route.Setup.route) { inclusive = true }
                }
            })
        }
        composable(Route.Home.route) {
            HomeScreen(
                onOpenNumbers = { navController.navigate(Route.Numbers.route) },
                onOpenList = { navController.navigate(Route.List.route) },
                onOpenListById = { id -> navController.navigate("list/$id") },
                onOpenDice = { navController.navigate(Route.Dice.route) },
                onOpenLot = { navController.navigate(Route.Lot.route) },
                onOpenCoin = { navController.navigate(Route.Coin.route) },
                onOpenSettings = { navController.navigate(Route.Settings.route) },
                onOpenAbout = { navController.navigate(Route.About.route) },
                onAddNumbersPreset = { /* TODO: screen for number presets */ },
                onAddListPreset = { navController.navigate(Route.AddListPreset.route) }
            )
        }
        composable(
            route = Route.Numbers.route,
            enterTransition = NavTransitions.enter,
            exitTransition = NavTransitions.exit,
            popEnterTransition = NavTransitions.popEnter,
            popExitTransition = NavTransitions.popExit
        ) {
            NumbersScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Route.Lot.route,
            enterTransition = NavTransitions.enter,
            exitTransition = NavTransitions.exit,
            popEnterTransition = NavTransitions.popEnter,
            popExitTransition = NavTransitions.popExit
        ) {
            LotScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Route.Dice.route,
            enterTransition = NavTransitions.enter,
            exitTransition = NavTransitions.exit,
            popEnterTransition = NavTransitions.popEnter,
            popExitTransition = NavTransitions.popExit
        ) {
            DiceScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Route.Coin.route,
            enterTransition = NavTransitions.enter,
            exitTransition = NavTransitions.exit,
            popEnterTransition = NavTransitions.popEnter,
            popExitTransition = NavTransitions.popExit
        ) {
            CoinScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Route.Settings.route,
            enterTransition = NavTransitions.enter,
            exitTransition = NavTransitions.exit,
            popEnterTransition = NavTransitions.popEnter,
            popExitTransition = NavTransitions.popExit
        ) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenGeneral = { navController.navigate(Route.SettingsGeneral.route) },
                onOpenAppearance = { navController.navigate(Route.SettingsAppearance.route) }
            )
        }
        composable(
            route = Route.SettingsGeneral.route,
            enterTransition = NavTransitions.enter,
            exitTransition = NavTransitions.exit,
            popEnterTransition = NavTransitions.popEnter,
            popExitTransition = NavTransitions.popExit
        ) {
            SettingsGeneralScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Route.SettingsAppearance.route,
            enterTransition = NavTransitions.enter,
            exitTransition = NavTransitions.exit,
            popEnterTransition = NavTransitions.popEnter,
            popExitTransition = NavTransitions.popExit
        ) {
            SettingsAppearanceScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Route.About.route,
            enterTransition = NavTransitions.enter,
            exitTransition = NavTransitions.exit,
            popEnterTransition = NavTransitions.popEnter,
            popExitTransition = NavTransitions.popExit
        ) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Route.List.route,
            enterTransition = NavTransitions.enter,
            exitTransition = NavTransitions.exit,
            popEnterTransition = NavTransitions.popEnter,
            popExitTransition = NavTransitions.popExit
        ) {
            ListScreen(
                onBack = { navController.popBackStack() },
                onOpenListById = { id ->
                    navController.navigate("list/$id") {
                        popUpTo(Route.Home.route) { inclusive = false }
                    }
                }
            )
        }
        composable(
            route = Route.ListWithId.route,
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
                onOpenListById = { nid -> navController.navigate("list/$nid") }
            )
        }
    }
}


