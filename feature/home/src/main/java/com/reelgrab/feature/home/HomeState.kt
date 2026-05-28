package com.reelgrab.feature.home

import com.reelgrab.domain.model.ErrorReason
import com.reelgrab.domain.model.Source

/**
 * UI model for the Home screen.
 *
 * Why a single state holder? The Home screen has several cross-cutting affordances (clipboard
 * hint, inline validation, fetch progress, error banner) that need to be observed atomically
 * by Compose. Keeping them on one data class avoids racing flows during recomposition.
 */
data class HomeState(
    val url: String = "",
    val urlValid: Boolean = false,
    val source: Source? = null,
    val clipboardSuggestion: String? = null,
    val isLoading: Boolean = false,
    val fetchError: ErrorReason? = null,
)
