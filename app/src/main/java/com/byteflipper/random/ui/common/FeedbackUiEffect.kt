package com.byteflipper.random.ui.common

import com.byteflipper.random.data.settings.HapticsIntensity

internal interface FeedbackUiEffect {
    val snackbarMessageRes: Int?
    val hapticIntensity: HapticsIntensity?
}
