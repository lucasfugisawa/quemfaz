package com.fugisawa.quemfaz.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Placeholder brand palette — replace with final brand colors when available.
private val PrimaryLight = Color(0xFF3F51B5)
private val OnPrimaryLight = Color(0xFFFFFFFF)
private val PrimaryContainerLight = Color(0xFFDBE1FF)
private val OnPrimaryContainerLight = Color(0xFF00164E)
private val SecondaryLight = Color(0xFF585E71)
private val OnSecondaryLight = Color(0xFFFFFFFF)
private val SecondaryContainerLight = Color(0xFFDCE2F9)
private val OnSecondaryContainerLight = Color(0xFF151B2C)
private val TertiaryLight = Color(0xFF735572)
private val OnTertiaryLight = Color(0xFFFFFFFF)
private val TertiaryContainerLight = Color(0xFFFED7FA)
private val OnTertiaryContainerLight = Color(0xFF2B122C)
private val ErrorLight = Color(0xFFBA1A1A)
private val OnErrorLight = Color(0xFFFFFFFF)
private val BackgroundLight = Color(0xFFFBF8FF)
private val OnBackgroundLight = Color(0xFF1B1B21)
private val SurfaceLight = Color(0xFFFBF8FF)
private val OnSurfaceLight = Color(0xFF1B1B21)

private val PrimaryDark = Color(0xFFB4C5FF)
private val OnPrimaryDark = Color(0xFF002A78)
private val PrimaryContainerDark = Color(0xFF243B8E)
private val OnPrimaryContainerDark = Color(0xFFDBE1FF)
private val SecondaryDark = Color(0xFFC0C6DD)
private val OnSecondaryDark = Color(0xFF2A3042)
private val SecondaryContainerDark = Color(0xFF404659)
private val OnSecondaryContainerDark = Color(0xFFDCE2F9)
private val TertiaryDark = Color(0xFFE1BBDE)
private val OnTertiaryDark = Color(0xFF422742)
private val TertiaryContainerDark = Color(0xFF5A3D59)
private val OnTertiaryContainerDark = Color(0xFFFED7FA)
private val ErrorDark = Color(0xFFFFB4AB)
private val OnErrorDark = Color(0xFF690005)
private val BackgroundDark = Color(0xFF131318)
private val OnBackgroundDark = Color(0xFFE4E1E9)
private val SurfaceDark = Color(0xFF131318)
private val OnSurfaceDark = Color(0xFFE4E1E9)

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
