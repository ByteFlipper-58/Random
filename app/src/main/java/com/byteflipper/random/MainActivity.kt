package com.byteflipper.random

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.byteflipper.random.ui.app.AppRoot
import com.byteflipper.random.ui.theme.RandomTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
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
import kotlinx.coroutines.flow.first
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.tasks.await
import com.byteflipper.random.ads.AppOpenAdManager
import com.byteflipper.random.ads.InterstitialAdManager

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var splashScreen: SplashScreenDecorator? = null

    private lateinit var appUpdateManager: AppUpdateManager
    private var installStateUpdatedListener: InstallStateUpdatedListener? = null
    private val updateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { /* no-op */ }
    private lateinit var interstitialAdManager: InterstitialAdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            showSplash()
        }
        setTheme(R.style.Theme_Random)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // enableEdgeToEdge уже управляет системными барами

        setContent {
            RandomTheme { AppRoot() }
        }
        interstitialAdManager = InterstitialAdManager(this)
        interstitialAdManager.preload()

        // In-App Update
        setupInAppUpdate()

        // Обработка изменения локали (собираем только при STARTED)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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

        lifecycleScope.launch {
            // Ожидаем готовность приложения (первую загрузку настроек)
            settingsRepository.settingsFlow.first()

            // Старт выезда сплэша
            splashScreen?.shouldKeepOnScreen = false

            // Дождаться завершения exit-анимации и фейда Compose-контента
            val totalExitMs = (SPLASH_EXIT_ANIM_MS + SPLASH_FADE_OFFSET_MS)
            delay(totalExitMs.milliseconds)
            splashScreen?.dismiss()

            // Триггерим In-App Review ненавязчиво (не чаще, чем при старте)
            maybeLaunchInAppReview()
        }
        // App Open Ads: сообщаем текущую Activity менеджеру
        (application as? RandomApplication)?.appOpenAdManager?.setCurrentActivity(this)

        // UMP: запрос информации о согласии и показ формы при необходимости
        (application as? RandomApplication)?.consentManager?.requestConsent(
            activity = this,
            onReadyForAds = { canRequest ->
                if (canRequest) {
                    // Разрешено запрашивать рекламу: загружаем App Open и межстраничные
                    (application as? RandomApplication)?.appOpenAdManager?.showAdIfAvailable()
                    interstitialAdManager.preload()
                }
            },
            onError = { _ ->
                // При ошибке — пробуем продолжить, если можно запрашивать рекламу
                if ((application as? RandomApplication)?.consentManager?.canRequestAds() == true) {
                    (application as? RandomApplication)?.appOpenAdManager?.showAdIfAvailable()
                    interstitialAdManager.preload()
                }
            }
        )
    }

    private fun showSplash() {
        splashScreen = splash {
            content {
                exitAnimationDuration = SPLASH_EXIT_ANIM_MS
                composeViewFadeDurationOffset = SPLASH_FADE_OFFSET_MS
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

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            val isUpdateAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val isFlexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            if (isUpdateAvailable && isFlexibleAllowed) {
                val options = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateLauncher,
                    options
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        installStateUpdatedListener?.let { appUpdateManager.registerListener(it) }
    }

    override fun onStop() {
        installStateUpdatedListener?.let { appUpdateManager.unregisterListener(it) }
        super.onStop()
    }

    private suspend fun maybeLaunchInAppReview() {
        runCatching {
            val reviewManager = ReviewManagerFactory.create(this)
            val reviewInfo = reviewManager.requestReviewFlow().await()
            reviewManager.launchReviewFlow(this, reviewInfo).await()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private companion object {
        const val SPLASH_EXIT_ANIM_MS: Long = 3200L
        const val SPLASH_FADE_OFFSET_MS: Long = 400L
    }
}