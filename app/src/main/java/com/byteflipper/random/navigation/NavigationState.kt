package com.byteflipper.random.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

class NavigationState internal constructor(
    val backStack: MutableList<NavKey>
)

@Composable
fun rememberNavigationState(initialKey: AppNavKey): NavigationState {
    val backStack = rememberNavBackStack(initialKey)
    return remember(backStack) { NavigationState(backStack) }
}
