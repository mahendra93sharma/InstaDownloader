package com.reelgrab.domain.model

/**
 * UI-facing state machine for the resolve-URL flow.
 *
 * Why: the Home / Preview screens crossfade between four mutually exclusive states (spec §3
 * Feedback states). Modelling this as a sealed interface makes `when` exhaustive in Compose and
 * removes the need for nullable `items` + boolean `isLoading` flags that historically drift out
 * of sync.
 */
sealed interface FetchState {
    /** No URL has been submitted yet (or the screen was just reset). */
    data object Idle : FetchState

    /** A resolve call is in-flight; UI should show the shimmer. */
    data object Loading : FetchState

    /** Resolve succeeded; [items] is non-empty per the extractor contract. */
    data class Success(val items: List<MediaItem>) : FetchState

    /** Resolve failed; UI maps [reason] to a localized message + retry affordance. */
    data class Error(val reason: ErrorReason) : FetchState
}
