package com.byteflipper.random.ads

import android.app.Activity
import android.content.Context
import com.byteflipper.random.BuildConfig
import com.byteflipper.random.consent.ConsentManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterstitialAdManager(
    context: Context,
    private val consentManager: ConsentManager,
    private val adsInitializer: AdsInitializer
) {
    companion object {
        private const val INTERSTITIAL_AD_TTL_MS: Long = 60 * 60 * 1000L
    }

    private val appContext = context.applicationContext
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var adLoadedAtMs: Long = 0

    private val testAdUnitId: String = "ca-app-pub-3940256099942544/1033173712"
    private val adUnitId: String = "ca-app-pub-4346225518624754/6107747651"

    fun preload() {
        clearExpiredAdIfNeeded()
        if (!consentManager.canRequestAds() || isLoading || interstitialAd != null) return
        adsInitializer.initializeIfNeeded {
            loadAd()
        }
    }

    private fun loadAd() {
        if (!consentManager.canRequestAds() || isLoading || interstitialAd != null) return

        isLoading = true
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            appContext,
            if (BuildConfig.DEBUG) testAdUnitId else adUnitId,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    adLoadedAtMs = System.currentTimeMillis()
                    isLoading = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    clearAd()
                    isLoading = false
                }
            }
        )
    }

    fun showIfAvailable(activity: Activity, onShown: () -> Unit = {}, onDismissed: () -> Unit = {}) {
        clearExpiredAdIfNeeded()
        if (!consentManager.canRequestAds()) {
            onDismissed()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            preload()
            onDismissed()
            return
        }
        clearAd()
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preload()
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                preload()
                onDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                onShown()
            }
        }
        ad.show(activity)
    }

    private fun clearExpiredAdIfNeeded() {
        if (interstitialAd != null && System.currentTimeMillis() - adLoadedAtMs >= INTERSTITIAL_AD_TTL_MS) {
            clearAd()
        }
    }

    private fun clearAd() {
        interstitialAd = null
        adLoadedAtMs = 0
    }
}


