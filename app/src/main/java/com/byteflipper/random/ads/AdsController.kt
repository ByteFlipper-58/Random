package com.byteflipper.random.ads

import android.app.Activity
import android.app.Application

class AdsController(application: Application) {
    private val appContext = application
    private val interstitialManager = InterstitialAdManager(application)

    private var numbersAndListsCount: Int = 0
    private var lotCount: Int = 0
    private var coinCount: Int = 0
    private var diceRollCount: Int = 0

    init {
        interstitialManager.preload()
    }

    fun onNumbersOrListsGenerated(activity: Activity) {
        val app = appContext as? com.byteflipper.random.RandomApplication
        if (app?.consentManager?.canRequestAds() == false) return
        numbersAndListsCount += 1
        if (numbersAndListsCount % 8 == 0) {
            interstitialManager.showIfAvailable(activity)
        } else {
            interstitialManager.preload()
        }
    }

    fun onLotGenerated(activity: Activity) {
        val app = appContext as? com.byteflipper.random.RandomApplication
        if (app?.consentManager?.canRequestAds() == false) return
        lotCount += 1
        if (lotCount % 6 == 0) {
            interstitialManager.showIfAvailable(activity)
        } else {
            interstitialManager.preload()
        }
    }

    fun onCoinTossed(activity: Activity) {
        val app = appContext as? com.byteflipper.random.RandomApplication
        if (app?.consentManager?.canRequestAds() == false) return
        coinCount += 1
        if (coinCount % 10 == 0) {
            interstitialManager.showIfAvailable(activity)
        } else {
            interstitialManager.preload()
        }
    }

    fun onDiceRolled(activity: Activity) {
        val app = appContext as? com.byteflipper.random.RandomApplication
        if (app?.consentManager?.canRequestAds() == false) return
        diceRollCount += 1
        if (diceRollCount % 8 == 0) {
            interstitialManager.showIfAvailable(activity)
        } else {
            interstitialManager.preload()
        }
    }
}


