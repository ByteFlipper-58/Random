package com.byteflipper.random

import android.app.Application
import com.byteflipper.random.ads.AdsController
import com.byteflipper.random.ads.AppOpenAdManager
import com.byteflipper.random.consent.ConsentManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject

@HiltAndroidApp
class RandomApplication : Application() {
    @Inject
    lateinit var appOpenAdManager: AppOpenAdManager
    @Inject lateinit var adsController: AdsController
    @Inject lateinit var consentManager: ConsentManager

    override fun onCreate() {
        super.onCreate()
        // Инициализация Google Mobile Ads SDK
        val config = RequestConfiguration.Builder()
            // Test Device ID for Emulator and potentially user's device
            .setTestDeviceIds(listOf(
                AdRequest.DEVICE_ID_EMULATOR, 
                "TEST_DEVICE_ID" // Placeholder, user might need to check logs for their specific ID
            ))
            .build()
        MobileAds.setRequestConfiguration(config)
        MobileAds.initialize(this)
    }
}
