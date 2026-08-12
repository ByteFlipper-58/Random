package com.byteflipper.random.ui.wheel

/**
 * Bounds for the number of wheel sectors.
 *
 * The minimum is hard: a single sector has a known outcome, and an empty wheel is a dead end the
 * user can only leave through "clear". Removal stops at [WHEEL_MIN_ITEMS] instead of letting the
 * list be scraped down to nothing.
 */
const val WHEEL_MIN_ITEMS = 2

/** Above this the sector labels stop being readable. */
const val WHEEL_MAX_ITEMS = 16
