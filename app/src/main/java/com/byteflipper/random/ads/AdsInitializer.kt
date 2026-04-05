package com.byteflipper.random.ads

import android.content.Context
import com.byteflipper.random.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdsInitializer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val lock = Any()
    private var configured = false
    private var initialized = false
    private var initializing = false
    private val pendingCallbacks = mutableListOf<() -> Unit>()

    fun initializeIfNeeded(onInitialized: () -> Unit = {}) {
        var invokeImmediately = false
        val shouldStartInitialization: Boolean

        synchronized(lock) {
            applyRequestConfigurationLocked()

            if (initialized) {
                invokeImmediately = true
                shouldStartInitialization = false
            } else {
                pendingCallbacks += onInitialized
                shouldStartInitialization = !initializing
                if (shouldStartInitialization) {
                    initializing = true
                }
            }
        }

        if (invokeImmediately) {
            onInitialized()
            return
        }

        if (!shouldStartInitialization) {
            return
        }

        MobileAds.initialize(context) {
            val callbacks = synchronized(lock) {
                initialized = true
                initializing = false
                pendingCallbacks.toList().also { pendingCallbacks.clear() }
            }
            callbacks.forEach { callback -> callback() }
        }
    }

    private fun applyRequestConfigurationLocked() {
        if (configured) {
            return
        }

        val builder = RequestConfiguration.Builder()
        if (BuildConfig.DEBUG) {
            builder.setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
        }

        MobileAds.setRequestConfiguration(builder.build())
        configured = true
    }
}
