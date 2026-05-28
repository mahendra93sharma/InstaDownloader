package com.reelgrab.feature.settings

import com.reelgrab.domain.model.Quality
import com.reelgrab.domain.model.ThemeMode

/**
 * Closed set of user intents for the Settings screen.
 *
 * Why an explicit `OnReviewDisclaimer` instead of a navigation callback? The Composable stays
 * platform-neutral and the ViewModel routes the request through [SettingsEffect] — the host
 * decides whether to navigate, open a dialog, or noop in tests.
 */
sealed interface SettingsEvent {
    data class OnThemeChange(val mode: ThemeMode) : SettingsEvent
    data class OnQualityChange(val quality: Quality) : SettingsEvent
    data object OnReviewDisclaimer : SettingsEvent
}
