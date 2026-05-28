package com.reelgrab.feature.settings

/**
 * One-shot effects emitted by [SettingsViewModel].
 *
 * Why no theme/quality side-effects? Those are pure state updates persisted by DataStore;
 * the UI re-renders by collecting the observer flow. Only navigation is a true effect.
 */
sealed interface SettingsEffect {
    data object NavigateToDisclaimer : SettingsEffect
}
