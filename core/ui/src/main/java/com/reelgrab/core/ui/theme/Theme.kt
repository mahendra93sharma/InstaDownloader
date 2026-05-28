package com.reelgrab.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Root theme wrapper used by every screen and `@Preview`.
 *
 * Why dynamic color by default? Material 3 expressive guidance — users have opted into
 * personal palettes at the OS level on Android 12+; honouring that builds trust and reduces
 * the visual surface we own. The brand `LightColors`/`DarkColors` are the fallback for
 * older OS, and the source of truth for Play Store screenshots where dynamic color is not
 * available.
 */
@Composable
fun ReelGrabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val supportsDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colors = when {
        supportsDynamic && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        supportsDynamic && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = ReelGrabTypography,
        shapes = ReelGrabShapes,
        content = content,
    )
}
