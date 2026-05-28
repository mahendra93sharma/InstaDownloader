package com.reelgrab.domain.model

/**
 * User preference for app theme, overriding `isSystemInDarkTheme()` when set explicitly.
 *
 * Why: spec §3 calls for full dark-mode support but also lets the user pin a mode in Settings.
 * Modelling this as an enum (rather than a nullable Boolean) makes the "follow system" case
 * explicit and reads cleanly in `when` blocks inside `ReelGrabTheme`.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
