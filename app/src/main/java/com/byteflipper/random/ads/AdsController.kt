package com.byteflipper.random.ads

import android.app.Activity
import android.app.Application
import com.byteflipper.random.BuildConfig
import com.byteflipper.random.consent.ConsentManager

class AdsController(
    private val application: Application,
    private val interstitialManager: InterstitialAdManager,
    private val consentManager: ConsentManager
) {

    private var numbersAndListsCount: Int = 0
    private var lotCount: Int = 0
    private var coinCount: Int = 0
    private var diceRollCount: Int = 0

    init {
        // Preload вызывается также и в MainActivity, но не помешает убедиться
        interstitialManager.preload()
    }

    fun onNumbersOrListsGenerated(activity: Activity) {
        if (!consentManager.canRequestAds()) return
        numbersAndListsCount += 1
        if (numbersAndListsCount % 8 == 0) {
            interstitialManager.showIfAvailable(activity)
        } else {
            interstitialManager.preload()
        }
    }

    fun onLotGenerated(activity: Activity) {
        if (!consentManager.canRequestAds()) return
        lotCount += 1
        if (lotCount % 6 == 0) {
            interstitialManager.showIfAvailable(activity)
        } else {
            interstitialManager.preload()
        }
    }

    fun onCoinTossed(activity: Activity) {
        if (!consentManager.canRequestAds()) return
        coinCount += 1
        if (coinCount % 8 == 0) {
            interstitialManager.showIfAvailable(activity)
        } else {
            interstitialManager.preload()
        }
    }

    fun onDiceRolled(activity: Activity) {
        if (!consentManager.canRequestAds()) return
        diceRollCount += 1
        if (diceRollCount % 6 == 0) {
            interstitialManager.showIfAvailable(activity)
        } else {
            interstitialManager.preload()
        }
    }
}


