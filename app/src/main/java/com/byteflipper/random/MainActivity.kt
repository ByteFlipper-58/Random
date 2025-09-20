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
import android.content.Intent
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var splashScreen: SplashScreenDecorator? = null

    private lateinit var appUpdateManager: AppUpdateManager
    private var installStateUpdatedListener: InstallStateUpdatedListener? = null
    private val updateRequestCode = 1001

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

        // In-App Update
        setupInAppUpdate()

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

        // Optionally trigger In-App Review (Google may ignore if not eligible)
        lifecycleScope.launch {
            delay(5.seconds)
            maybeLaunchInAppReview()
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

    private fun setupInAppUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(this)
        installStateUpdatedListener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                // Завершить установку загруженного обновления
                appUpdateManager.completeUpdate()
            }
        }
        installStateUpdatedListener?.let { appUpdateManager.registerListener(it) }

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            val isUpdateAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val isFlexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            if (isUpdateAvailable && isFlexibleAllowed) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    this,
                    updateRequestCode
                )
            }
        }
    }

    private fun maybeLaunchInAppReview() {
        val reviewManager = ReviewManagerFactory.create(this)
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                reviewManager.launchReviewFlow(this, reviewInfo).addOnCompleteListener {
                    // Ничего не делаем, результат не предоставляет статус
                }
            }
        }
    }

    

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == updateRequestCode) {
            // Пользователь мог отменить обновление; ничего не делаем
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        installStateUpdatedListener?.let { appUpdateManager.unregisterListener(it) }
    }
}