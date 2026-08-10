package com.byteflipper.random.app.startup

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.data.settings.ThemeMode
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class AppearanceController(
    private val activity: AppCompatActivity,
    private val splashScreen: SplashScreen,
    private val settingsRepository: SettingsRepository
) {
    fun start(animateSplash: Boolean) {
        var keepSplashOnScreen = animateSplash

        if (animateSplash) {
            splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                val alpha = android.animation.ObjectAnimator.ofFloat(
                    splashScreenView.view,
                    android.view.View.ALPHA,
                    1f,
                    0f
                )
                alpha.duration = 200L
                alpha.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        splashScreenView.remove()
                    }
                })
                alpha.start()
            }
        }

        activity.lifecycleScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            AppCompatDelegate.setDefaultNightMode(settings.themeMode.toNightMode())
            keepSplashOnScreen = false
        }
    }
}

internal fun AppCompatActivity.observeActivityAppearance(
    settingsRepository: SettingsRepository
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                settingsRepository.settingsFlow
                    .map { it.themeMode }
                    .distinctUntilChanged()
                    .collect { mode ->
                        AppCompatDelegate.setDefaultNightMode(mode.toNightMode())
                    }
            }

            launch {
                settingsRepository.settingsFlow
                    .map { it.appLanguage.localeTag }
                    .distinctUntilChanged()
                    .collect { tag ->
                        val desiredLocales = if (tag == "system") {
                            LocaleListCompat.getEmptyLocaleList()
                        } else {
                            LocaleListCompat.forLanguageTags(tag)
                        }

                        val currentLocales = AppCompatDelegate.getApplicationLocales()
                        if (currentLocales != desiredLocales) {
                            AppCompatDelegate.setApplicationLocales(desiredLocales)
                        }
                    }
            }
        }
    }
}

private fun ThemeMode.toNightMode(): Int {
    return when (this) {
        ThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
        ThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
        ThemeMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}
