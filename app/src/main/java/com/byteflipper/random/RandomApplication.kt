package com.byteflipper.random

import android.app.Application
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class RandomApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Google Mobile Ads SDK on a background thread
        CoroutineScope(Dispatchers.IO).launch {
            val config = RequestConfiguration.Builder()
                // Test Device ID for Emulator and potentially user's device
                .setTestDeviceIds(listOf(
                    AdRequest.DEVICE_ID_EMULATOR, 
                    "TEST_DEVICE_ID" // Placeholder, user might need to check logs for their specific ID
                ))
                .build()
            MobileAds.setRequestConfiguration(config)
            MobileAds.initialize(this@RandomApplication)
        }
    }
}
