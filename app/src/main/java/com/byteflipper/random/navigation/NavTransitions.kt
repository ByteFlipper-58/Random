package com.byteflipper.random.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.graphics.TransformOrigin
import androidx.navigation.NavBackStackEntry

object NavTransitions {
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300))
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { -it / 3 },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { -it / 3 },
            animationSpec = tween(500)
        ) + fadeIn(animationSpec = tween(200)) // Fade in quickly to be visible behind the shrinking top screen
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(durationMillis = 300, delayMillis = 200, easing = FastOutSlowInEasing)
        ) + scaleOut(
            targetScale = 0.9f,
            transformOrigin = TransformOrigin(0.5f, 0.5f), // Center shrink might look better for "padding creation", or stick to 1f, 0.5f
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = 300, delayMillis = 200))
    }
}


