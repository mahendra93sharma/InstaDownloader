package com.reelgrab.domain.model

/**
 * Closed taxonomy of failure modes surfaced to the user.
 *
 * Why: a sealed hierarchy lets the UI exhaustively map errors to localized strings in
 * `core/ui/ErrorMessages.kt` without ever relying on raw exception messages (which leak
 * implementation details and break with library upgrades). Every layer below the ViewModel
 * is responsible for collapsing its own exceptions into one of these cases — see spec section 6.
 */
sealed class ErrorReason {
    /** URL did not match either platform regex, or was blank. */
    data object InvalidUrl : ErrorReason()

    /** URL matched a known host but points at a kind of content this app cannot resolve. */
    data object Unsupported : ErrorReason()

    /** IO/timeout/DNS failure while talking to the extraction service or CDN. */
    data object Network : ErrorReason()

    /** Content is gated behind a login or visible only to followers. */
    data object PrivateContent : ErrorReason()

    /** Extraction backend or CDN replied with 429 / rate-limit semantics. */
    data object RateLimited : ErrorReason()

    /** Anything else; the cause is kept for logging but never shown to the user verbatim. */
    data class Unknown(val cause: Throwable? = null) : ErrorReason()
}
