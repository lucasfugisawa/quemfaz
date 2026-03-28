package com.fugisawa.quemfaz.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import quemfaz.composeapp.generated.resources.Res
import quemfaz.composeapp.generated.resources.inter_bold
import quemfaz.composeapp.generated.resources.inter_medium
import quemfaz.composeapp.generated.resources.inter_regular
import quemfaz.composeapp.generated.resources.inter_semibold

// ── App font family ─────────────────────────────────────────────────
// Single source of truth for the app typeface.
// To swap fonts later, replace only the body of this composable.
val AppFontFamily: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold),
    )

// ── Typography scale ────────────────────────────────────────────────
// Built at composition time so it picks up the composable AppFontFamily.
val AppTypography: Typography
    @Composable get() {
        val family = AppFontFamily
        return Typography(
            // Display — rarely used in-app; kept for completeness
            displayLarge = TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Normal,
                fontSize = 57.sp,
                lineHeight = 64.sp,
                letterSpacing = (-0.25).sp,
            ),
            displayMedium = TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Normal,
                fontSize = 45.sp,
                lineHeight = 52.sp,
            ),
            displaySmall = TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Normal,
                fontSize = 36.sp,
                lineHeight = 44.sp,
            ),

            // Headlines — page-level titles
            headlineLarge = TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.SemiBold,
                fontSize = 30.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.2).sp,
            ),
            headlineMedium = TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.15).sp,
            ),
            headlineSmall = TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 30.sp,
            ),

            // Titles — section / card titles
            titleLarge = TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
            ),
            titleMedium = TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.1.sp,
            ),
            titleSmall = TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),

            // Body — main readable content
            bodyLarge = TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
            ),
            bodyMedium = TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.15.sp,
            ),
            bodySmall = TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.2.sp,
            ),

            // Labels — buttons, chips, nav, metadata
            labelLarge = TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
            labelMedium = TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.25.sp,
            ),
            labelSmall = TextStyle(
                fontFamily = family,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.3.sp,
            ),
        )
    }
