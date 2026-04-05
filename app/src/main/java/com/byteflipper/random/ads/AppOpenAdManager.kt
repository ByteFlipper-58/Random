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
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class AppOpenAdManager(
    private val application: Application,
    private val consentManager: com.byteflipper.random.consent.ConsentManager,
    private val adsInitializer: AdsInitializer
) : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = AtomicBoolean(false)
    private var isLoadingAd = AtomicBoolean(false)
    private var currentActivityRef: WeakReference<Activity>? = null

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
        // При возврате приложения из фона показываем рекламу
        showAdIfAvailable()
    }

    /**
     * Загружает рекламу.
     * @param onAdLoadedCallback коллбэк, который будет вызван при успешной загрузке.
     * Используется для сценария "Cold Start": загрузили -> сразу показали.
     */
    private fun loadAd(onAdLoadedCallback: (() -> Unit)? = null) {
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
                        isLoadingAd.set(false)
                        onAdLoadedCallback?.invoke()
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isLoadingAd.set(false)
                    }
                }
            )
        }
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null
    }

    fun showAdIfAvailable() {
        // Если реклама уже есть — показываем
        if (isAdAvailable()) {
            showAdInternal()
        } else {
            // Если рекламы нет — загружаем и просим показать сразу после загрузки
            loadAd { showAdInternal() }
        }
    }

    private fun showAdInternal() {
        // Проверка согласия перед показом
        if (!consentManager.canRequestAds()) return
        
        // Без активности показывать нечего
        val activity = currentActivityRef?.get() ?: return

        if (isShowingAd.get()) return
        // На всякий случай проверяем наличие (могло пропасть, пока грузилось, хотя вряд ли в single thread)
        val ad = appOpenAd ?: return

        ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isShowingAd.set(false)
                appOpenAd = null
                // После закрытия — подгружаем следующую (уже без немедленного показа)
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isShowingAd.set(false)
                appOpenAd = null
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd.set(true)
            }
        }
        isShowingAd.set(true)
        ad.show(activity)
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


