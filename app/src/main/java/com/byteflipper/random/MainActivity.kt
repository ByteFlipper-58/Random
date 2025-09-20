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
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.byteflipper.random.data.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

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

        // Обработка изменения локали
        lifecycleScope.launch {
            settingsRepository.settingsFlow
                .map { it.appLanguage.localeTag }
                .distinctUntilChanged()
                .collect { tag ->
                    val desiredLocales = if (tag == "system") {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(tag)
                    }

                    // Проверяем, нужно ли обновлять локаль
                    val currentLocales = AppCompatDelegate.getApplicationLocales()
                    if (currentLocales != desiredLocales) {
                        AppCompatDelegate.setApplicationLocales(desiredLocales)
                    }
                }
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