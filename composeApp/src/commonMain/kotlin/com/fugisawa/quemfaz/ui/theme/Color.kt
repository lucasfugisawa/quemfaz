package com.fugisawa.quemfaz.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// QuemFaz brand palette
private val PrimaryLight = Color(0xFF1B6CA8)
private val OnPrimaryLight = Color(0xFFFFFFFF)
private val PrimaryContainerLight = Color(0xFFC8E4F5)
private val OnPrimaryContainerLight = Color(0xFF003459)
private val SecondaryLight = Color(0xFF2E7D32)
private val OnSecondaryLight = Color(0xFFFFFFFF)
private val SecondaryContainerLight = Color(0xFFC8E6C9)
private val OnSecondaryContainerLight = Color(0xFF003300)
private val TertiaryLight = Color(0xFFF57C00)
private val OnTertiaryLight = Color(0xFFFFFFFF)
private val TertiaryContainerLight = Color(0xFFFFE0B2)
private val OnTertiaryContainerLight = Color(0xFF4A2800)
private val ErrorLight = Color(0xFFC62828)
private val OnErrorLight = Color(0xFFFFFFFF)
private val BackgroundLight = Color(0xFFF5F5FA)
private val OnBackgroundLight = Color(0xFF1A1A2E)
private val SurfaceLight = Color(0xFFFFFFFF)
private val OnSurfaceLight = Color(0xFF1A1A2E)

private val PrimaryDark = Color(0xFF93C5E8)
private val OnPrimaryDark = Color(0xFF003459)
private val PrimaryContainerDark = Color(0xFF1B5282)
private val OnPrimaryContainerDark = Color(0xFFC8E4F5)
private val SecondaryDark = Color(0xFF81C784)
private val OnSecondaryDark = Color(0xFF003300)
private val SecondaryContainerDark = Color(0xFF1B5E20)
private val OnSecondaryContainerDark = Color(0xFFC8E6C9)
private val TertiaryDark = Color(0xFFFFB74D)
private val OnTertiaryDark = Color(0xFF4A2800)
private val TertiaryContainerDark = Color(0xFFBF5900)
private val OnTertiaryContainerDark = Color(0xFFFFE0B2)
private val ErrorDark = Color(0xFFFFB4AB)
private val OnErrorDark = Color(0xFF690005)
private val BackgroundDark = Color(0xFF131318)
private val OnBackgroundDark = Color(0xFFF5F5FA)
private val SurfaceDark = Color(0xFF1A1A2E)
private val OnSurfaceDark = Color(0xFFF5F5FA)

// Contact button colors
val WhatsAppGreen = Color(0xFF22C55E)
val CallBlue = Color(0xFF3B82F6)

// Category-colored service tags
val TagBlueBg = Color(0xFFDBEAFE)
val TagBlueText = Color(0xFF1D4ED8)
val TagGreenBg = Color(0xFFDCFCE7)
val TagGreenText = Color(0xFF15803D)
val TagPurpleBg = Color(0xFFF3E8FF)
val TagPurpleText = Color(0xFF7E22CE)
val TagOrangeBg = Color(0xFFFFEDD5)
val TagOrangeText = Color(0xFFC2410C)

val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
)

val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
)
