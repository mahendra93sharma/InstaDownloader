package com.reelgrab.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Brand palette + Material 3 color schemes.
 *
 * Why custom values? Dynamic color (Android 12+) is the default; these schemes are the
 * brand-recognisable fallback on older OS versions and the source of truth for marketing
 * surfaces. Picking concrete hex codes here (instead of relying solely on dynamic) keeps
 * the visual identity stable across previews, screenshots, and Play Store assets.
 */
internal val BrandPrimary = Color(0xFF7C3AED)
internal val BrandPrimaryContainer = Color(0xFFEDE0FF)
internal val BrandSecondary = Color(0xFF0EA5E9)
internal val BrandSecondaryContainer = Color(0xFFCFEEFE)
internal val BrandTertiary = Color(0xFFEC4899)
internal val BrandError = Color(0xFFB3261E)

val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = Color(0xFF22074D),
    secondary = BrandSecondary,
    onSecondary = Color.White,
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = Color(0xFF002A3D),
    tertiary = BrandTertiary,
    onTertiary = Color.White,
    background = Color(0xFFFBFAFD),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFBFAFD),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    error = BrandError,
    onError = Color.White,
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFC4A8FF),
    onPrimary = Color(0xFF3A0F84),
    primaryContainer = Color(0xFF5A22B8),
    onPrimaryContainer = BrandPrimaryContainer,
    secondary = Color(0xFF7CD2F8),
    onSecondary = Color(0xFF003448),
    secondaryContainer = Color(0xFF004C68),
    onSecondaryContainer = BrandSecondaryContainer,
    tertiary = Color(0xFFFFB1C8),
    onTertiary = Color(0xFF5E1133),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)
