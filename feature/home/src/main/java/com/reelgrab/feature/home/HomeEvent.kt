package com.reelgrab.feature.home

/**
 * Closed set of user intents accepted by [HomeViewModel].
 *
 * Why a sealed class? Every UI affordance (text edit, paste, submit, dismiss) maps to a single
 * `onEvent` entry point; sealed dispatch keeps the ViewModel exhaustive and the Composable free
 * of business logic.
 */
sealed interface HomeEvent {
    data class OnUrlChange(val value: String) : HomeEvent
    data class OnPaste(val value: String) : HomeEvent
    data object OnClear : HomeEvent
    data object OnSubmit : HomeEvent
    data object OnDismissClipboardHint : HomeEvent
    data object OnAcceptClipboardSuggestion : HomeEvent
}
