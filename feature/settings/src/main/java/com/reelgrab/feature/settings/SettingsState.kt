package com.reelgrab.feature.settings

import com.reelgrab.domain.model.Quality
import com.reelgrab.domain.model.ThemeMode

/**
 * UI model for the Settings screen.
 *
 * Why three flat fields instead of nested groups? Settings UI is dictated by sections in the
 * Composable; the state object stays simple so each preference can be observed from DataStore
 * and dropped in via the `combine` operator without an extra mapping layer.
 */
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultQuality: Quality = Quality.ORIGINAL,
    val disclaimerAccepted: Boolean = false,
)
