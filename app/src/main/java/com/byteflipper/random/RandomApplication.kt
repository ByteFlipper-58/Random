package com.byteflipper.random

import android.app.Application
import com.byteflipper.random.data.settings.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RandomApplication : Application() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
    }
}
