package com.reelgrab.domain.repository

import com.reelgrab.domain.model.DownloadRecord
import kotlinx.coroutines.flow.Flow

/**
 * Boundary over the Room database that persists completed downloads.
 *
 * Why: the History screen needs to survive process death, so it can't rely on WorkManager's
 * in-memory state. This interface exposes a Flow of records (so Compose can collect with
 * `collectAsStateWithLifecycle`) plus mutation hooks that the [com.reelgrab.domain.usecase
 * .DownloadMediaUseCase] uses when a worker reports `Completed`.
 */
interface HistoryRepository {
    /** Cold stream of all persisted records, newest first. */
    fun stream(): Flow<List<DownloadRecord>>

    /** Persist a freshly completed download. */
    suspend fun add(record: DownloadRecord)

    /** Remove a single record by id; the underlying media file is left intact. */
    suspend fun delete(id: String)

    /** Substring search over `sourceUrl`; returns an immediate snapshot (no Flow). */
    suspend fun search(query: String): List<DownloadRecord>
}
