package com.byteflipper.random.ads

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.byteflipper.random.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class AppOpenAdManager(private val application: Application) : DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = AtomicBoolean(false)
    private var isLoadingAd = AtomicBoolean(false)
    private var currentActivityRef: WeakReference<Activity>? = null

    // Тестовый рекламный блок App Open
    private val testAdUnitId: String = "ca-app-pub-3940256099942544/9257395921"
    private val adUnitId: String = "ca-app-pub-4346225518624754/9085813527"

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun setCurrentActivity(activity: Activity?) {
        currentActivityRef = if (activity == null) null else WeakReference(activity)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // Показывать при возобновлении приложения
        showAdIfAvailable()
    }

    private fun loadAd() {
        // Не загружаем, если нет согласия на запрос рекламы
        val app = application as? com.byteflipper.random.RandomApplication
        if (app?.consentManager?.canRequestAds() == false) return
        if (isLoadingAd.get() || appOpenAd != null) return
        isLoadingAd.set(true)
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            application,
            if (BuildConfig.DEBUG) testAdUnitId else adUnitId,
            request,
            object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd.set(false)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd.set(false)
                }
            }
        )
    }

    fun showAdIfAvailable() {
        val app = application as? com.byteflipper.random.RandomApplication
        if (app?.consentManager?.canRequestAds() == false) return
        val activity = currentActivityRef?.get() ?: return loadAd()
        if (isShowingAd.get()) return

        if (appOpenAd == null) {
            loadAd()
            return
        }

        isShowingAd.set(true)
        appOpenAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isShowingAd.set(false)
                appOpenAd = null
                // После закрытия — подгружаем следующую
                Handler(Looper.getMainLooper()).post { loadAd() }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isShowingAd.set(false)
                appOpenAd = null
                Handler(Looper.getMainLooper()).post { loadAd() }
            }

            override fun onAdShowedFullScreenContent() {
                // no-op
            }
        }

        appOpenAd?.show(activity)
    }
}


