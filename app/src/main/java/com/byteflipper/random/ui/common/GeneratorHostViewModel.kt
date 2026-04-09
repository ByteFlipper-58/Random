package com.byteflipper.random.ui.common

internal interface GeneratorHostViewModel {
    fun getEffectiveDelayMs(): Int

    fun randomizeCardColor()

    fun setOverlayVisible(visible: Boolean)

    fun notifyHapticPressIfEnabled()

    fun checkAd(activity: android.app.Activity)
}
