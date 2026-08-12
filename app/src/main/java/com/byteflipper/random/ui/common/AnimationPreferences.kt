package com.byteflipper.random.ui.common

import android.content.Context
import android.database.ContentObserver
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Whether system animations are enabled.
 *
 * They can be turned off from developer options or by "remove animations" in accessibility
 * settings, sometimes for speed and sometimes because on-screen motion makes the user sick.
 * Long animations should be shortened in that case rather than played in full.
 *
 * The value is observed, so toggling the setting while the app is running is picked up.
 */
@Composable
fun rememberAnimationsEnabled(): State<Boolean> {
    val context = LocalContext.current
    val enabled = remember { mutableStateOf(context.animatorDurationScale() != 0f) }

    DisposableEffect(context) {
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                enabled.value = context.animatorDurationScale() != 0f
            }
        }
        context.contentResolver.registerContentObserver(uri, false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    return enabled
}

private fun Context.animatorDurationScale(): Float =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
