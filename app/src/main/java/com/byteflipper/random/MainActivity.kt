package com.byteflipper.random

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.activity.viewModels
import com.byteflipper.random.ads.AppOpenAdManager
import com.byteflipper.random.ads.InterstitialAdManager
import com.byteflipper.random.consent.ConsentManager
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.data.settings.ThemeMode
import com.byteflipper.random.review.InAppReviewManager
import com.byteflipper.random.ui.app.AppRoot
import com.byteflipper.random.ui.app.AppViewModel
import com.byteflipper.random.ui.app.PendingSharedImport
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val appViewModel: AppViewModel by viewModels()

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private lateinit var appUpdateManager: AppUpdateManager
    private var installStateUpdatedListener: InstallStateUpdatedListener? = null
    private val updateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { /* no-op */ }
    @Inject
    lateinit var interstitialAdManager: InterstitialAdManager
    @Inject
    lateinit var appOpenAdManager: AppOpenAdManager
    @Inject
    lateinit var consentManager: ConsentManager
    @Inject
    lateinit var inAppReviewManager: InAppReviewManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                inAppReviewManager.onSessionStarted()
            }
        }
        // Keep splash screen visible until settings are loaded
        var keepSplashOnScreen = true
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

        lifecycleScope.launch {
            // Wait for settings to load
            val settings = settingsRepository.settingsFlow.first()
            val mode = settings.themeMode
            val nightMode = when (mode) {
                ThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                ThemeMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(nightMode)
            
            keepSplashOnScreen = false
        }

        enableEdgeToEdge()
        // Theme is set via postSplashScreenTheme in styles, but we can enforce it here
        // Set transparent theme or just rely on RandomTheme usage in Compose.
        // The reference usually does NOT call setTheme if postSplashScreenTheme works correctly.
        // However, standard activity often needs a theme.
        // We will stick to EdgeToEdge + Compose.

        setContent {
            AppRoot()
        }

        handleIncomingIntent(intent)

        // In-App Update
        setupInAppUpdate()

        // Locale handling and Theme Mode observation
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    settingsRepository.settingsFlow
                        .map { it.themeMode }
                        .distinctUntilChanged()
                        .collect { mode ->
                            val nightMode = when (mode) {
                                ThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
                                ThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                                ThemeMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            }
                            AppCompatDelegate.setDefaultNightMode(nightMode)
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

        // UMP Consent
        consentManager.requestConsent(
            activity = this,
            onReadyForAds = { canRequest ->
                if (canRequest) {
                    appOpenAdManager.preload()
                    interstitialAdManager.preload()
                }
            },
            onError = { _ ->
                if (consentManager.canRequestAds()) {
                    appOpenAdManager.preload()
                    interstitialAdManager.preload()
                }
            }
        )
    }

    private fun setupInAppUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(this)
        installStateUpdatedListener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onStop() {
        installStateUpdatedListener?.let { appUpdateManager.unregisterListener(it) }
        super.onStop()
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val incomingIntent = intent ?: return
        when (incomingIntent.action) {
            Intent.ACTION_SEND -> {
                val streamUri = incomingIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    ?: incomingIntent.firstUriFromClipData()
                val text = incomingIntent.getStringExtra(Intent.EXTRA_TEXT)
                if (streamUri != null || !text.isNullOrBlank()) {
                    appViewModel.submitSharedImport(
                        PendingSharedImport(
                            uri = streamUri,
                            text = text?.takeIf { it.isNotBlank() },
                            label = incomingIntent.getStringExtra(Intent.EXTRA_TITLE)
                                ?: incomingIntent.getStringExtra(Intent.EXTRA_SUBJECT)
                        )
                    )
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val streamUri = incomingIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    ?.firstOrNull()
                    ?: incomingIntent.firstUriFromClipData()
                val text = incomingIntent.getStringExtra(Intent.EXTRA_TEXT)
                if (streamUri != null || !text.isNullOrBlank()) {
                    appViewModel.submitSharedImport(
                        PendingSharedImport(
                            uri = streamUri,
                            text = text?.takeIf { it.isNotBlank() },
                            label = incomingIntent.getStringExtra(Intent.EXTRA_TITLE)
                                ?: incomingIntent.getStringExtra(Intent.EXTRA_SUBJECT)
                        )
                    )
                }
            }

            Intent.ACTION_VIEW -> {
                val viewUri = incomingIntent.data ?: incomingIntent.firstUriFromClipData()
                viewUri?.let { uri ->
                    appViewModel.submitSharedImport(PendingSharedImport(uri = uri))
                }
            }
        }
    }

    private fun Intent.firstUriFromClipData(): Uri? {
        val clipData = clipData ?: return null
        return (0 until clipData.itemCount)
            .asSequence()
            .mapNotNull { index -> clipData.getItemAt(index).uri }
            .firstOrNull()
    }
}
