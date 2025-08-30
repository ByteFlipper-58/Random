package com.byteflipper.random.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.byteflipper.random.ui.home.HomeScreen
import com.byteflipper.random.ui.numbers.NumbersScreen
import com.byteflipper.random.ui.lists.ListScreen
import com.byteflipper.random.ui.coin.CoinScreen
import com.byteflipper.random.ui.lot.LotScreen
import com.byteflipper.random.ui.dice.DiceScreen
import com.byteflipper.random.ui.settings.SettingsScreen
import com.byteflipper.random.ui.about.AboutScreen

sealed class Route(val route: String) {
    data object Home : Route("home")
    data object Numbers : Route("numbers")
    data object List : Route("list")
    data object ListWithId : Route("list/{id}")
    data object AddListPreset : Route("add_list_preset")
    data object Dice : Route("dice")
    data object Lot : Route("lot")
    data object Coin : Route("coin")
    data object Settings : Route("settings")
    data object About : Route("about")
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Route.Home.route) {
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
            enterTransition = {
                scaleIn(initialScale = 0.92f) + fadeIn()
            },
            exitTransition = {
                scaleOut(targetScale = 1.06f) + fadeOut()
            },
            popEnterTransition = {
                scaleIn(initialScale = 1.06f) + fadeIn()
            },
            popExitTransition = {
                scaleOut(targetScale = 0.92f) + fadeOut()
            }
        ) {
            NumbersScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Route.Lot.route,
            enterTransition = {
                scaleIn(initialScale = 0.92f) + fadeIn()
            },
            exitTransition = {
                scaleOut(targetScale = 1.06f) + fadeOut()
            },
            popEnterTransition = {
                scaleIn(initialScale = 1.06f) + fadeIn()
            },
            popExitTransition = {
                scaleOut(targetScale = 0.92f) + fadeOut()
            }
        ) {
            LotScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Route.Dice.route,
            enterTransition = {
                scaleIn(initialScale = 0.92f) + fadeIn()
            },
            exitTransition = {
                scaleOut(targetScale = 1.06f) + fadeOut()
            },
            popEnterTransition = {
                scaleIn(initialScale = 1.06f) + fadeIn()
            },
            popExitTransition = {
                scaleOut(targetScale = 0.92f) + fadeOut()
            }
        ) {
            DiceScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Route.Coin.route,
            enterTransition = {
                scaleIn(initialScale = 0.92f) + fadeIn()
            },
            exitTransition = {
                scaleOut(targetScale = 1.06f) + fadeOut()
            },
            popEnterTransition = {
                scaleIn(initialScale = 1.06f) + fadeIn()
            },
            popExitTransition = {
                scaleOut(targetScale = 0.92f) + fadeOut()
            }
        ) {
            CoinScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Route.Settings.route,
            enterTransition = {
                scaleIn(initialScale = 0.92f) + fadeIn()
            },
            exitTransition = {
                scaleOut(targetScale = 1.06f) + fadeOut()
            },
            popEnterTransition = {
                scaleIn(initialScale = 1.06f) + fadeIn()
            },
            popExitTransition = {
                scaleOut(targetScale = 0.92f) + fadeOut()
            }
        ) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Route.About.route,
            enterTransition = {
                scaleIn(initialScale = 0.92f) + fadeIn()
            },
            exitTransition = {
                scaleOut(targetScale = 1.06f) + fadeOut()
            },
            popEnterTransition = {
                scaleIn(initialScale = 1.06f) + fadeIn()
            },
            popExitTransition = {
                scaleOut(targetScale = 0.92f) + fadeOut()
            }
        ) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Route.List.route,
            enterTransition = {
                scaleIn(initialScale = 0.92f) + fadeIn()
            },
            exitTransition = {
                scaleOut(targetScale = 1.06f) + fadeOut()
            },
            popEnterTransition = {
                scaleIn(initialScale = 1.06f) + fadeIn()
            },
            popExitTransition = {
                scaleOut(targetScale = 0.92f) + fadeOut()
            }
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
            enterTransition = {
                scaleIn(initialScale = 0.92f) + fadeIn()
            },
            exitTransition = {
                scaleOut(targetScale = 1.06f) + fadeOut()
            },
            popEnterTransition = {
                scaleIn(initialScale = 1.06f) + fadeIn()
            },
            popExitTransition = {
                scaleOut(targetScale = 0.92f) + fadeOut()
            }
        ) {
            val id = it.arguments?.getString("id")?.toLongOrNull()
            ListScreen(
                onBack = { navController.popBackStack() },
                presetId = id,
                onOpenListById = { nid -> navController.navigate("list/$nid") }
            )
        }
    }
}


