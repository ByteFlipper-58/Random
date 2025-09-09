package com.byteflipper.random

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.byteflipper.random.ui.app.AppRoot
import com.byteflipper.random.ui.theme.RandomTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.byteflipper.random.ui.setup.HeartBeatAnimation
import net.kibotu.splashscreen.SplashScreenDecorator
import net.kibotu.splashscreen.splash
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var splashScreen: SplashScreenDecorator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            showSplash()
        }
        setTheme(R.style.Theme_Random)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            RandomTheme { AppRoot() }
        }

        lifecycleScope.launch {
            delay(1.seconds)
            splashScreen?.shouldKeepOnScreen = false
            delay(3.seconds)
            splashScreen?.dismiss()
        }
    }

    private fun showSplash() {
        val exitDuration = 800L
        val fadeDurationOffset = 200L

        splashScreen = splash {
            content {
                exitAnimationDuration = exitDuration
                composeViewFadeDurationOffset = fadeDurationOffset
                RandomTheme {
                    HeartBeatAnimation(
                        isVisible = isVisible.value,
                        exitAnimationDuration = exitAnimationDuration.milliseconds,
                        onStartExitAnimation = { startExitAnimation() }
                    )
                }
            }
        }
    }
}