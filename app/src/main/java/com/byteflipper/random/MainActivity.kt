package com.byteflipper.random

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.data.settings.ThemeMode
import com.byteflipper.random.data.settings.AppLanguage
import com.byteflipper.random.navigation.AppNavGraph
import com.byteflipper.random.ui.components.HeartBeatAnimation
import com.byteflipper.random.ui.theme.RandomTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kibotu.splashscreen.SplashScreenDecorator
import net.kibotu.splashscreen.splash
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var splashScreen: SplashScreenDecorator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // показываем кастомный сплэш до super.onCreate()
        showSplash()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Настраиваем статус-бар по умолчанию для светлой темы (до загрузки настроек)
        window.statusBarColor = android.graphics.Color.argb(128, 255, 255, 255)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }

        // Применяем сохраненный язык
        applySavedLanguage()

        setContent {
            val context = LocalContext.current
            val settings: Settings by settingsRepository.settingsFlow.collectAsState(initial = Settings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            // Настраиваем цвет статус-бара при изменении темы
            LaunchedEffect(darkTheme) {
                window.statusBarColor = if (darkTheme) {
                    android.graphics.Color.TRANSPARENT
                } else {
                    // Для светлой темы делаем статус-бар полупрозрачным с темным текстом
                    android.graphics.Color.argb(128, 255, 255, 255)
                }
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                }
            }

            RandomTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavGraph(navController = navController)
                }
            }
        }

        lifecycleScope.launch {
            // 1) даём 1 секунду стандартному AndroidX Splash API
            delay(1.seconds)
            splashScreen?.shouldKeepOnScreen = false

            // 2) держим кастомную анимацию 3 секунды
            delay(3.seconds)

            // 3) запускаем анимацию выхода HeartBeatAnimation
            splashScreen?.dismiss()
        }
    }

    private fun applySavedLanguage() {
        lifecycleScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                val localeList = when (settings.appLanguage) {
                    AppLanguage.System -> LocaleListCompat.getEmptyLocaleList()
                    AppLanguage.English -> LocaleListCompat.forLanguageTags("en")
                    AppLanguage.Russian -> LocaleListCompat.forLanguageTags("ru")
                }
                AppCompatDelegate.setApplicationLocales(localeList)
            }
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