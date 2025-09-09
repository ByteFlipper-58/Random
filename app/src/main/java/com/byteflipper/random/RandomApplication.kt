package com.byteflipper.random

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RandomApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
