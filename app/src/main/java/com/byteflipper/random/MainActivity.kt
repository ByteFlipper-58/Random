package com.byteflipper.random

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.activity.viewModels
import com.byteflipper.random.app.intent.SharedImportIntentHandler
import com.byteflipper.random.app.startup.AppearanceController
import com.byteflipper.random.app.startup.UpdateCoordinator
import com.byteflipper.random.app.startup.bootstrapConsentAndAds
import com.byteflipper.random.app.startup.observeActivityAppearance
import com.byteflipper.random.app.startup.trackStartupSessionStart
import com.byteflipper.random.ads.AppOpenAdManager
import com.byteflipper.random.ads.InterstitialAdManager
import com.byteflipper.random.consent.ConsentManager
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.review.InAppReviewManager
import com.byteflipper.random.ui.app.AppRoot
import com.byteflipper.random.ui.app.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val appViewModel: AppViewModel by viewModels()

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val updateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { /* no-op */ }
    private lateinit var updateCoordinator: UpdateCoordinator
    private val intentHandler by lazy { SharedImportIntentHandler(appViewModel) }
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
        updateCoordinator = UpdateCoordinator(
            activity = this,
            updateLauncher = updateLauncher
        )

        AppearanceController(
            activity = this,
            splashScreen = splashScreen,
            settingsRepository = settingsRepository
        ).start()
        trackStartupSessionStart(
            savedInstanceState = savedInstanceState,
            inAppReviewManager = inAppReviewManager
        )

        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(
                LocalNavigationEventDispatcherOwner provides this@MainActivity
            ) {
                AppRoot()
            }
        }

        intentHandler.handle(intent)
        updateCoordinator.setup()
        observeActivityAppearance(settingsRepository)
        bootstrapConsentAndAds(
            consentManager = consentManager,
            appOpenAdManager = appOpenAdManager,
            interstitialAdManager = interstitialAdManager
        )
    }

    override fun onStart() {
        super.onStart()
        updateCoordinator.onStart()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentHandler.handle(intent)
    }

    override fun onStop() {
        updateCoordinator.onStop()
        super.onStop()
    }
}
