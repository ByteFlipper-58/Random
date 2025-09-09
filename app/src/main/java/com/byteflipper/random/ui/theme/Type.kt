package com.byteflipper.random.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GoogleFontTypeface
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.byteflipper.random.R

private val montserrat = GoogleFont("Montserrat")
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)

val MontserratFamily = FontFamily(
    GoogleFontTypeface(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Black),
    GoogleFontTypeface(googleFont = montserrat, fontProvider = provider, weight = FontWeight.ExtraBold),
    GoogleFontTypeface(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Bold),
    GoogleFontTypeface(googleFont = montserrat, fontProvider = provider, weight = FontWeight.SemiBold),
    GoogleFontTypeface(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Medium),
    GoogleFontTypeface(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Normal),
    GoogleFontTypeface(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Light),
)

private val NoPad = PlatformTextStyle(includeFontPadding = false)

val ExpTitleTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 60.sp,
        textGeometricTransform = TextGeometricTransform(scaleX = 1.5f),
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em,
        platformStyle = NoPad
    ),
    displayMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 50.sp,
        //textGeometricTransform = TextGeometricTransform(scaleX = 1f),
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em,
        platformStyle = NoPad
    ),
    titleMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        textGeometricTransform = TextGeometricTransform(scaleX = 1.3f),
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em,
        platformStyle = NoPad
    )
)

// Define tu FontFamily personalizada aquí
val GoogleSansRounded = FontFamily(
    Font(R.font.google_sans_rounded_regular, FontWeight.Normal)
    // Agrega otras variantes (light, medium, italic) si las tienes
)

// Set of Material typography styles to start with
// Базовая типографика приложения (используется на всех экранах, кроме карточек списков/чисел)
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 60.sp,
        letterSpacing = (-0.02).em,
        lineHeight = 1.0.em,
        textGeometricTransform = TextGeometricTransform(scaleX = 1.15f),
        platformStyle = NoPad
    ),
    displayMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 48.sp,
        letterSpacing = (-0.01).em,
        lineHeight = 1.05.em,
        platformStyle = NoPad
    ),
    displaySmall = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        letterSpacing = 0.em,
        lineHeight = 1.05.em,
        platformStyle = NoPad
    ),
    headlineLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        letterSpacing = (-0.01).em,
        lineHeight = 1.1.em,
        platformStyle = NoPad
    ),
    headlineMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        letterSpacing = 0.em,
        lineHeight = 1.1.em,
        platformStyle = NoPad
    ),
    headlineSmall = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        letterSpacing = 0.em,
        lineHeight = 1.1.em,
        platformStyle = NoPad
    ),
    titleLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.01).em,
        lineHeight = 1.15.em,
        platformStyle = NoPad
    ),
    titleMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = (-0.005).em,
        lineHeight = 1.2.em,
        platformStyle = NoPad
    ),
    titleSmall = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.em,
        lineHeight = 1.2.em,
        platformStyle = NoPad
    ),
    bodyLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        lineHeight = 1.35.em,
        platformStyle = NoPad
    ),
    bodyMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = 1.35.em,
        platformStyle = NoPad
    ),
    bodySmall = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.sp,
        lineHeight = 1.35.em,
        platformStyle = NoPad
    ),
    labelLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.em,
        lineHeight = 1.1.em,
        platformStyle = NoPad
    ),
    labelMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.05.em,
        lineHeight = 1.1.em,
        platformStyle = NoPad
    ),
    labelSmall = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.05.em,
        lineHeight = 1.1.em,
        platformStyle = NoPad
    ),
)

// Компактная типографика для карточек на экранах списков и чисел
val CardTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 44.sp,
        letterSpacing = 0.em,
        lineHeight = 1.0.em,
        platformStyle = NoPad
    ),
    displayMedium = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        letterSpacing = 0.em,
        lineHeight = 1.0.em,
        platformStyle = NoPad
    ),
    displaySmall = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        letterSpacing = 0.em,
        lineHeight = 1.0.em,
        platformStyle = NoPad
    ),
    headlineLarge = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        letterSpacing = 0.em,
        lineHeight = 1.1.em,
        platformStyle = NoPad
    ),
    headlineMedium = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        letterSpacing = 0.em,
        lineHeight = 1.1.em,
        platformStyle = NoPad
    ),
    headlineSmall = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        letterSpacing = 0.em,
        lineHeight = 1.1.em,
        platformStyle = NoPad
    ),
    titleLarge = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        letterSpacing = 0.em,
        lineHeight = 1.15.em,
        platformStyle = NoPad
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.em,
        lineHeight = 1.15.em,
        platformStyle = NoPad
    ),
    titleSmall = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.em,
        lineHeight = 1.15.em,
        platformStyle = NoPad
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = 1.25.em,
        platformStyle = NoPad
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.sp,
        lineHeight = 1.25.em,
        platformStyle = NoPad
    ),
    bodySmall = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.sp,
        lineHeight = 1.25.em,
        platformStyle = NoPad
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.05.em,
        lineHeight = 1.1.em,
        platformStyle = NoPad
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.05.em,
        lineHeight = 1.1.em,
        platformStyle = NoPad
    ),
    labelSmall = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.05.em,
        lineHeight = 1.1.em,
        platformStyle = NoPad
    ),
)

@Composable
fun CardContentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = CardTypography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}