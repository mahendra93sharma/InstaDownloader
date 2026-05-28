package com.reelgrab.feature.home

/**
 * One-shot effects emitted by [HomeViewModel].
 *
 * Why a Channel-backed effect instead of state? Navigation and snackbars are not idempotent —
 * replaying them on config change would cause duplicate navigation. Effects fire exactly once.
 */
sealed interface HomeEffect {
    data class NavigateToPreview(val encodedUrl: String) : HomeEffect
    data class ShowSnackbar(val message: String) : HomeEffect
}
