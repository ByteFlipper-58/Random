package com.byteflipper.random.utils

/**
 * Constants used throughout the application
 */
object Constants {

    // Database
    const val DATABASE_NAME = "random.db"

    // DataStore
    const val DATASTORE_SETTINGS_NAME = "user_settings"

    // DataStore keys for default list
    const val DEFAULT_LIST_NAME_KEY = "default_list_name"
    const val DEFAULT_LIST_ITEMS_KEY = "default_list_items"
    const val ITEMS_SEPARATOR = "\u0001"

    // Generator settings
    const val DEFAULT_DELAY_MS = 3000
    const val MIN_DELAY_MS = 500
    const val MAX_DELAY_MS = 10000
    const val INSTANT_DELAY_MS = 1000

    // UI constants
    const val MIN_GENERATE_COUNT = 1
    const val MAX_GENERATE_COUNT = 1000
    const val DEFAULT_GENERATE_COUNT = 1

    // Animation durations
    const val SPLASH_FADE_DURATION = 800L
    const val SPLASH_FADE_OFFSET = 200L
    const val SPLASH_ANDROIDX_DURATION = 1000L
    const val SPLASH_CUSTOM_DURATION = 3000L

    // Card sizes
    const val LIST_CARD_SIZE_DP = 320
    const val NUMBERS_CARD_SIZE_DP = 280

    // Adaptive UI calculations
    const val ADAPTIVE_PADDING_RATIO = 0.04f
    const val ADAPTIVE_SPACING_RATIO = 0.03f
    const val ADAPTIVE_TITLE_FONT_RATIO = 0.045f
    const val ADAPTIVE_ITEM_FONT_RATIO = 0.035f

    // Adaptive UI bounds
    const val MIN_ADAPTIVE_PADDING_DP = 12f
    const val MAX_ADAPTIVE_PADDING_DP = 28f
    const val MIN_ADAPTIVE_SPACING_DP = 6f
    const val MAX_ADAPTIVE_SPACING_DP = 16f
    const val MIN_TITLE_FONT_SP = 18f
    const val MAX_TITLE_FONT_SP = 32f
    const val MIN_ITEM_FONT_SP = 20f
    const val MAX_ITEM_FONT_SP = 36f

    // Scrim animation
    const val SCRIM_BLUR_MULTIPLIER = 8f

    // Haptic feedback
    const val HAPTIC_FEEDBACK_ENABLED = true
}
