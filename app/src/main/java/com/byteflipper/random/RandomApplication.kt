package com.byteflipper.random

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import dagger.hilt.android.HiltAndroidApp
import com.byteflipper.random.ads.AppOpenAdManager
import com.byteflipper.random.ads.AdsController
import com.byteflipper.random.consent.ConsentManager

@HiltAndroidApp
class RandomApplication : Application() {
    lateinit var appOpenAdManager: AppOpenAdManager
    lateinit var adsController: AdsController
    lateinit var consentManager: ConsentManager
    override fun onCreate() {
        super.onCreate()
        // Инициализация Google Mobile Ads SDK
        val config = RequestConfiguration.Builder()
            // TODO: добавьте ID тестовых устройств, если нужно: .setTestDeviceIds(listOf("TEST_DEVICE_ID"))
            .build()
        MobileAds.setRequestConfiguration(config)
        MobileAds.initialize(this)

        consentManager = ConsentManager(this)
        appOpenAdManager = AppOpenAdManager(this)
        adsController = AdsController(this)
    }
}
