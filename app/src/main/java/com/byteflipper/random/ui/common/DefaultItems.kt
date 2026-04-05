package com.byteflipper.random.ui.common

import android.content.Context
import com.byteflipper.random.R

fun Context.defaultRandomItems(): List<String> = listOf(
    getString(R.string.item_1),
    getString(R.string.item_2),
    getString(R.string.item_3)
)
