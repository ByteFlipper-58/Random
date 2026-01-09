package com.byteflipper.random.ads

import android.app.Activity
import com.byteflipper.random.BuildConfig
import com.byteflipper.random.consent.ConsentManager

class AdsController(
    private val interstitialManager: InterstitialAdManager,
    private val consentManager: ConsentManager
) {

    private var numbersAndListsCount: Int = 0
    private var lotCount: Int = 0
    private var coinCount: Int = 0
    private var diceRollCount: Int = 0

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


