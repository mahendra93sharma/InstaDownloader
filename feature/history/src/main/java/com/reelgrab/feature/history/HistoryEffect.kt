package com.reelgrab.feature.history

/**
 * One-shot effects emitted by [HistoryViewModel].
 *
 * Why separate `OpenUri` and `ShareUri`? Both end up dispatching Android intents, but the
 * receiver (gallery vs. share sheet) and the lifecycle (single-shot vs. user-cancellable)
 * are different enough that fusing them would force callers to branch on a string.
 */
sealed interface HistoryEffect {
    data class OpenUri(val uri: String, val mimeType: String) : HistoryEffect
    data class ShareUri(val uri: String, val mimeType: String) : HistoryEffect
    data class ShowSnackbar(val message: String) : HistoryEffect
}
