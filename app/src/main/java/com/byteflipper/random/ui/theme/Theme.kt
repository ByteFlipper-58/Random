package com.byteflipper.random.ui.theme

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.byteflipper.random.ui.theme.model.Theme
import com.byteflipper.random.ui.theme.model.ThemeContrast

@Composable
fun RandomTheme(
    theme: Theme = Theme.DYNAMIC,
    darkTheme: Boolean = isSystemInDarkTheme(),
    isPureDark: Boolean = false,
    themeContrast: ThemeContrast = ThemeContrast.STANDARD,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Disabling Autofill (optional)
             window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS

            // Fix for nav bar being semi transparent in api 29+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val colorScheme = colorScheme(
        theme = theme,
        darkTheme = darkTheme,
        isPureDark = isPureDark,
        themeContrast = themeContrast
    )
    val animatedColorScheme = animateColorScheme(targetColorScheme = colorScheme)

    CompositionLocalProvider(LocalRandomColorScheme provides animatedColorScheme) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            typography = Typography,
            content = content
        )
    }
}

internal val LocalRandomColorScheme = staticCompositionLocalOf<ColorScheme> {
    error("RandomTheme is not present")
}
