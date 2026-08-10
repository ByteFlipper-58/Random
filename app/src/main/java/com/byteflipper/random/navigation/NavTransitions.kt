package com.byteflipper.random.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.TransformOrigin

object NavTransitions {
    fun forward(): ContentTransform =
        (slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        ) + scaleIn(
            initialScale = 0.9f,
            transformOrigin = TransformOrigin.Center,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(400))) togetherWith
            (slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300)))

    fun backward(): ContentTransform =
        (slideInHorizontally(
            initialOffsetX = { -it / 3 },
            animationSpec = tween(500)
        ) + fadeIn(animationSpec = tween(200))) togetherWith
            (slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = 200,
                    easing = FastOutSlowInEasing
                )
            ) + scaleOut(
                targetScale = 0.9f,
                transformOrigin = TransformOrigin.Center,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 300, delayMillis = 200)))
}
