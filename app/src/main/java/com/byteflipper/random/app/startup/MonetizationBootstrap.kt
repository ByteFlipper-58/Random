package com.byteflipper.random.app.startup

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.byteflipper.random.ads.AppOpenAdManager
import com.byteflipper.random.ads.InterstitialAdManager
import com.byteflipper.random.consent.ConsentManager
import com.byteflipper.random.review.InAppReviewManager
import kotlinx.coroutines.launch

internal fun AppCompatActivity.trackStartupSessionStart(
    savedInstanceState: Bundle?,
    inAppReviewManager: InAppReviewManager
) {
    if (savedInstanceState == null) {
        lifecycleScope.launch {
            inAppReviewManager.onSessionStarted()
        }
    }
}

internal fun AppCompatActivity.bootstrapConsentAndAds(
    consentManager: ConsentManager,
    appOpenAdManager: AppOpenAdManager,
    interstitialAdManager: InterstitialAdManager
) {
    fun preloadAds() {
        appOpenAdManager.preload()
        interstitialAdManager.preload()
    }

    consentManager.requestConsent(
        activity = this,
        onReadyForAds = { canRequest ->
            if (canRequest) {
                preloadAds()
            }
        },
        onError = { _ ->
            if (consentManager.canRequestAds()) {
                preloadAds()
            }
        }
    )
}
