package com.oddjobs.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

val Primary = Color(0xFF0E5C4A)
val PrimaryDark = Color(0xFF0A4435)
val PrimaryLight = Color(0xFFE8F5F0)
val Accent = Color(0xFFC4922E)
val AccentLight = Color(0xFFFDF5E3)
val Canvas = Color(0xFFF6F4EE)
val CardColor = Color(0xFFFFFFFF)
val Border = Color(0xFFE8E3D8)
val Ink = Color(0xFF1C1917)
val Muted = Color(0xFF6F6A64)
val Warn = Color(0xFFEA580C)
val WarnBg = Color(0xFFFFF4E5)
val Success = Color(0xFF16A34A)
val SuccessBg = Color(0xFFE9F9EF)
val ErrorRed = Color(0xFFD92D20)
val ErrorBg = Color(0xFFFDECEA)

private val Colors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = Accent,
    onSecondary = Color.White,
    secondaryContainer = AccentLight,
    onSecondaryContainer = Ink,
    background = Canvas,
    onBackground = Ink,
    surface = CardColor,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0ECE2),
    onSurfaceVariant = Muted,
    outline = Border,
    outlineVariant = Border,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun OddJobsTheme(content: @Composable () -> Unit) {
    // Bengali glyphs need generous line height, so every text style gets extra leading.
    val base = Typography()
    val typography = base.copy(
        headlineLarge = base.headlineLarge.copy(lineHeight = 42.sp),
        headlineMedium = base.headlineMedium.copy(lineHeight = 38.sp),
        headlineSmall = base.headlineSmall.copy(lineHeight = 34.sp),
        titleLarge = base.titleLarge.copy(lineHeight = 32.sp),
        titleMedium = base.titleMedium.copy(lineHeight = 26.sp),
        titleSmall = base.titleSmall.copy(lineHeight = 22.sp),
        bodyLarge = base.bodyLarge.copy(lineHeight = 26.sp),
        bodyMedium = base.bodyMedium.copy(lineHeight = 22.sp),
        bodySmall = base.bodySmall.copy(lineHeight = 18.sp),
        labelLarge = base.labelLarge.copy(lineHeight = 22.sp),
        labelMedium = base.labelMedium.copy(lineHeight = 18.sp)
    )
    MaterialTheme(colorScheme = Colors, typography = typography, content = content)
}
