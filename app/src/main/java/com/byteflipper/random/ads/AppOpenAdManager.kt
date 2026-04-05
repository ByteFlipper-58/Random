package com.byteflipper.random.ads

import android.app.Activity
import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.byteflipper.random.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class AppOpenAdManager(
    private val application: Application,
    private val consentManager: com.byteflipper.random.consent.ConsentManager,
    private val adsInitializer: AdsInitializer
) : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {
    companion object {
        private const val APP_OPEN_AD_TTL_MS: Long = 4 * 60 * 60 * 1000L
    }

    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = AtomicBoolean(false)
    private var isLoadingAd = AtomicBoolean(false)
    private var currentActivityRef: WeakReference<Activity>? = null
    private var adLoadedAtMs: Long = 0
    private var hasSeenInitialForeground = false
    private var appWasBackgrounded = false
    private var skipNextForegroundAd = false

    // Тестовый рекламный блок App Open
    private val testAdUnitId: String = "ca-app-pub-3940256099942544/9257395921"
    private val adUnitId: String = "ca-app-pub-4346225518624754/9085813527"

    init {
        // Следим за жизненным циклом процесса (для запуска при возврате из фона)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        // Следим за активностями (для контекста показа)
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (!hasSeenInitialForeground) {
            hasSeenInitialForeground = true
            preload()
            return
        }

        if (skipNextForegroundAd) {
            skipNextForegroundAd = false
            preload()
            return
        }

        if (!appWasBackgrounded) {
            preload()
            return
        }

        appWasBackgrounded = false
        showAdIfAvailable()
    }

    override fun onStop(owner: LifecycleOwner) {
        appWasBackgrounded = true
        super.onStop(owner)
    }

    fun preload() {
        loadAd()
    }

    private fun loadAd() {
        if (!consentManager.canRequestAds()) return
        if (isLoadingAd.get() || isAdAvailable()) return

        adsInitializer.initializeIfNeeded {
            if (!consentManager.canRequestAds() || isLoadingAd.get() || isAdAvailable()) {
                return@initializeIfNeeded
            }

            isLoadingAd.set(true)
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                application,
                if (BuildConfig.DEBUG) testAdUnitId else adUnitId,
                request,
                object : AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        adLoadedAtMs = System.currentTimeMillis()
                        isLoadingAd.set(false)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        clearAd()
                        isLoadingAd.set(false)
                    }
                }
            )
        }
    }

    private fun isAdAvailable(): Boolean {
        if (appOpenAd == null) {
            return false
        }

        if (System.currentTimeMillis() - adLoadedAtMs >= APP_OPEN_AD_TTL_MS) {
            clearAd()
            return false
        }

        return true
    }

    fun showAdIfAvailable() {
        if (isShowingAd.get()) return

        if (isAdAvailable()) {
            showAdInternal()
            return
        }

        preload()
    }

    private fun showAdInternal() {
        if (!consentManager.canRequestAds()) return

        val activity = currentActivityRef?.get() ?: return

        if (isShowingAd.get()) return
        val ad = appOpenAd ?: return
        if (!isAdAvailable()) {
            preload()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isShowingAd.set(false)
                clearAd()
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isShowingAd.set(false)
                clearAd()
                preload()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd.set(true)
            }

            override fun onAdClicked() {
                skipNextForegroundAd = true
            }
        }
        isShowingAd.set(true)
        ad.show(activity)
    }

    private fun clearAd() {
        appOpenAd = null
        adLoadedAtMs = 0
    }

    // --- ActivityLifecycleCallbacks Implementation ---

    override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivityRef?.get() == activity) {
            currentActivityRef = null
        }
    }
}


