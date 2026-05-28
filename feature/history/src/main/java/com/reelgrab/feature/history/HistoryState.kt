package com.reelgrab.feature.history

import com.reelgrab.domain.model.DownloadRecord

/**
 * UI model for the History tab.
 *
 * Why hold both `records` and `query` here? The filtered list rendered by Compose is derived
 * by the ViewModel via `combine(records, query)` so the screen only needs to read this single
 * source. Keeping the query in state (rather than `remember`-ed in the Composable) ensures it
 * survives config change without resorting to `rememberSaveable` per text field.
 */
data class HistoryState(
    val records: List<DownloadRecord> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
)
