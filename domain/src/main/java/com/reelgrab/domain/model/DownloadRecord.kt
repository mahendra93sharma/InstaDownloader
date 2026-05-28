package com.reelgrab.domain.model

/**
 * Persisted record of a completed download, surfaced on the History screen.
 *
 * Why: a `DownloadStatus.Completed` is ephemeral — it only lives as long as the WorkManager
 * record. Once the worker finishes, the use case writes a [DownloadRecord] into Room so the
 * History tab can show it after process death. The fields are kept transport-neutral (no
 * `Uri`, no `Date`) so the domain layer doesn't drag in Android types.
 *
 * - [savedUri] is the `content://` URI stringified; the data layer rehydrates it for re-open.
 * - [savedAt] is epoch milliseconds — store/sort agnostic to local timezone.
 */
data class DownloadRecord(
    val id: String,
    val sourceUrl: String,
    val savedUri: String,
    val type: MediaType,
    val savedAt: Long,
    val sizeBytes: Long,
)
