package com.byteflipper.random.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
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
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Black),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.ExtraBold),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Light),
)

val ExpTitleTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 60.sp,
        textGeometricTransform = TextGeometricTransform(scaleX = 1.5f),
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    ),
    displayMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 50.sp,
        //textGeometricTransform = TextGeometricTransform(scaleX = 1f),
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    ),
    titleMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        textGeometricTransform = TextGeometricTransform(scaleX = 1.3f),
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
)

// Define tu FontFamily personalizada aquí
val GoogleSansRounded = FontFamily(
    Font(R.font.google_sans_rounded_regular, FontWeight.Normal)
    // Agrega otras variantes (light, medium, italic) si las tienes
)

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)