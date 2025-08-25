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
import com.byteflipper.random.ui.presets.AddListPresetScreen

sealed class Route(val route: String) {
    data object Home : Route("home")
    data object Numbers : Route("numbers")
    data object List : Route("list")
    data object AddListPreset : Route("add_list_preset")
    data object Dice : Route("dice")
    data object Lot : Route("lot")
    data object Coin : Route("coin")
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Route.Home.route) {
        composable(Route.Home.route) {
            HomeScreen(
                onOpenNumbers = { navController.navigate(Route.Numbers.route) },
                onOpenList = { /* TODO */ },
                onOpenDice = { /* TODO */ },
                onOpenLot = { /* TODO */ },
                onOpenCoin = { /* TODO */ },
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
            route = Route.AddListPreset.route,
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
            AddListPresetScreen(onBack = { navController.popBackStack() })
        }
    }
}


