package com.reelgrab.feature.preview

import com.reelgrab.domain.model.DownloadStatus
import com.reelgrab.domain.model.FetchState

/**
 * UI model for the preview / gallery screen.
 *
 * Why a `Map<itemId, DownloadStatus>`? Multiple media items can be downloading concurrently —
 * a flat map lets each tile observe its own status without LazyGrid recomposing every cell on
 * any progress update.
 */
data class PreviewState(
    val url: String,
    val fetch: FetchState = FetchState.Loading,
    val downloads: Map<String, DownloadStatus> = emptyMap(),
)
