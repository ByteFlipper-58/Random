package com.byteflipper.random.di

import android.app.Application
import android.content.Context
import com.byteflipper.random.ads.AdsController
import com.byteflipper.random.ads.AppOpenAdManager
import com.byteflipper.random.ads.InterstitialAdManager
import com.byteflipper.random.consent.ConsentManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdsModule {

    @Provides
    @Singleton
    fun provideAppOpenAdManager(
        application: Application,
        consentManager: ConsentManager
    ): AppOpenAdManager {
        return AppOpenAdManager(application, consentManager)
    }

    @Provides
    @Singleton
    fun provideInterstitialAdManager(@ApplicationContext context: Context): InterstitialAdManager {
        return InterstitialAdManager(context)
    }

    @Provides
    @Singleton
    fun provideAdsController(
        interstitialAdManager: InterstitialAdManager,
        consentManager: ConsentManager
    ): AdsController {
        return AdsController(interstitialAdManager, consentManager)
    }

    @Provides
    @Singleton
    fun provideConsentManager(@ApplicationContext context: Context): ConsentManager {
        return ConsentManager(context)
    }
}
