package com.reelgrab.feature.history

import com.reelgrab.domain.model.DownloadRecord

/**
 * Closed taxonomy of user intents accepted by [HistoryViewModel].
 *
 * Why sealed? Each row has only three actions plus a search query — making the set exhaustive
 * means adding a future affordance (e.g. "re-download") fails compilation in the `when` block
 * until the ViewModel handles it.
 */
sealed interface HistoryEvent {
    data class OnSearchChange(val query: String) : HistoryEvent
    data class OnDelete(val id: String) : HistoryEvent
    data class OnOpen(val record: DownloadRecord) : HistoryEvent
    data class OnShare(val record: DownloadRecord) : HistoryEvent
}
