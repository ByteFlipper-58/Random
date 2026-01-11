package com.byteflipper.random.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.byteflipper.random.ui.theme.color.*
import com.byteflipper.random.ui.theme.model.Theme
import com.byteflipper.random.ui.theme.model.ThemeContrast

@Composable
fun colorScheme(
    theme: Theme,
    darkTheme: Boolean,
    isPureDark: Boolean, // Placeholder for pure dark mode if we want it, or unused for now
    themeContrast: ThemeContrast
): ColorScheme {
    val colorScheme = when (theme) {
        Theme.DYNAMIC -> {
            dynamicTheme(isDark = darkTheme)
        }
        else -> {
            // Fallback to BlueTheme for all other themes for now
            blueTheme(isDark = darkTheme, themeContrast = themeContrast)
        }
    }

    // Pure dark implementation logic (placeholder or basic implementation from reference can go here)
    // For now we just return the scheme. 
    // If we want to support pure dark (OLED black), we can uncomment the logic below if we had BlackTheme.kt
    /*
    return if (isPureDark && darkTheme) {
        blackTheme(initialTheme = colorScheme)
    } else {
        colorScheme
    }
    */
    return colorScheme
}

fun getRainbowColors(): List<Color> {
    return listOf(
        Color(0xFFE57373),
        Color(0xFFF06292),
        Color(0xFFBA68C8),
        Color(0xFF9575CD),
        Color(0xFF7986CB),
        Color(0xFF64B5F6),
        Color(0xFF4FC3F7),
        Color(0xFF4DD0E1),
        Color(0xFF4DB6AC),
        Color(0xFF81C784),
        Color(0xFFAED581),
        Color(0xFFFFD54F),
        Color(0xFFFFB74D),
        Color(0xFFFF8A65),
        Color(0xFFA1887F),
        Color(0xFF90A4AE)
    )
}
