package com.byteflipper.random.ads

import android.app.Activity
import com.byteflipper.random.consent.ConsentManager
import com.byteflipper.random.review.InAppReviewManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AdsController(
    private val interstitialManager: InterstitialAdManager,
    private val consentManager: ConsentManager,
    private val inAppReviewManager: InAppReviewManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var numbersAndListsCount: Int = 0
    private var lotCount: Int = 0
    private var coinCount: Int = 0
    private var diceRollCount: Int = 0
    private var wheelSpinCount: Int = 0

    fun onNumbersOrListsGenerated(activity: Activity) {
        handleSuccessfulAction(activity, nextCount = {
            numbersAndListsCount += 1
            numbersAndListsCount
        }, interstitialFrequency = 8)
    }

    fun onLotGenerated(activity: Activity) {
        handleSuccessfulAction(activity, nextCount = {
            lotCount += 1
            lotCount
        }, interstitialFrequency = 6)
    }

    fun onCoinTossed(activity: Activity) {
        handleSuccessfulAction(activity, nextCount = {
            coinCount += 1
            coinCount
        }, interstitialFrequency = 8)
    }

    fun onDiceRolled(activity: Activity) {
        handleSuccessfulAction(activity, nextCount = {
            diceRollCount += 1
            diceRollCount
        }, interstitialFrequency = 6)
    }

    fun onWheelSpun(activity: Activity) {
        handleSuccessfulAction(activity, nextCount = {
            wheelSpinCount += 1
            wheelSpinCount
        }, interstitialFrequency = 8)
    }

    private fun handleSuccessfulAction(
        activity: Activity,
        nextCount: () -> Int,
        interstitialFrequency: Int
    ) {
        scope.launch {
            val reviewAttempted = inAppReviewManager.onSuccessfulAction(activity)
            if (reviewAttempted) {
                interstitialManager.preload()
                return@launch
            }

            if (!consentManager.canRequestAds()) {
                return@launch
            }

            val count = nextCount()
            if (count % interstitialFrequency == 0) {
                interstitialManager.showIfAvailable(activity)
            } else {
                interstitialManager.preload()
            }
        }
    }
}


