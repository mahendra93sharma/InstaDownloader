package com.reelgrab.domain.model

/**
 * Per-item lifecycle of a download orchestrated by WorkManager.
 *
 * Why: the UI needs a typed stream — not raw `WorkInfo.State` — so it can render different
 * affordances per state (progress bar, "Open" action on completion, retry on failure). The
 * `itemId` is duplicated on every variant so a single `Flow<List<DownloadStatus>>` can drive
 * the entire history pane without joining against the item list.
 */
sealed interface DownloadStatus {
    /** The id of the [MediaItem] this status belongs to. */
    val itemId: String

    /** Enqueued, not yet running (waiting on constraints or worker pool). */
    data class Queued(override val itemId: String) : DownloadStatus

    /** Streaming bytes; [progressPct] is 0..100 inclusive. */
    data class Running(override val itemId: String, val progressPct: Int) : DownloadStatus

    /** Persisted in MediaStore; [uri] is the content:// URI the gallery / open intent uses. */
    data class Completed(override val itemId: String, val uri: String) : DownloadStatus

    /** Download failed; UI shows the mapped error and a retry button. */
    data class Failed(override val itemId: String, val reason: ErrorReason) : DownloadStatus
}
