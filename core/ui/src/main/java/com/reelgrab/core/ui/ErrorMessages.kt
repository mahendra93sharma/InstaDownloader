package com.reelgrab.core.ui

import com.reelgrab.domain.model.ErrorReason

/**
 * Single mapping from the domain [ErrorReason] taxonomy to user-facing strings.
 *
 * Why here? The agent rules pin this to `core/ui/ErrorMessages.kt` so every feature surface
 * (Home banner, Preview card, History row) shows identical wording. Localization later swaps
 * this body to `stringResource` lookups without touching call sites.
 */
fun ErrorReason.userMessage(): String = when (this) {
    is ErrorReason.InvalidUrl -> "That doesn't look like an Instagram or Facebook link."
    is ErrorReason.Unsupported -> "This kind of content isn't supported yet."
    is ErrorReason.Network -> "Couldn't reach the server. Check your connection."
    is ErrorReason.PrivateContent -> "This post is private. Public links only."
    is ErrorReason.RateLimited -> "Too many requests. Please wait a moment and try again."
    is ErrorReason.Unknown -> "Something went wrong. Please try again."
}
