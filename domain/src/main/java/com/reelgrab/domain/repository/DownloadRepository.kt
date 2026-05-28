package com.reelgrab.domain.repository

import com.reelgrab.domain.model.DownloadRequest
import com.reelgrab.domain.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

/**
 * Boundary over WorkManager + MediaStore for enqueuing and observing downloads.
 *
 * Why: the use cases must not import `androidx.work.*` (keeps domain pure JVM) and the UI must
 * not poll work IDs by hand. Exposing both a per-job [observe] and an aggregate [observeAll]
 * lets the Preview screen track one item while the History screen subscribes to everything.
 */
interface DownloadRepository {
    /**
     * Submit [request] for background download. Returns a stable work id that callers pass to
     * [observe]. Implementations MUST tag the work so [observeAll] can surface it too.
     */
    fun enqueue(request: DownloadRequest): String

    /**
     * Stream of status updates for a single work id. The flow completes when the worker reaches
     * a terminal state (`Completed` or `Failed`).
     */
    fun observe(workId: String): Flow<DownloadStatus>

    /**
     * Cold stream of every currently-tracked download. Used by the global progress / history UIs.
     */
    fun observeAll(): Flow<List<DownloadStatus>>
}
