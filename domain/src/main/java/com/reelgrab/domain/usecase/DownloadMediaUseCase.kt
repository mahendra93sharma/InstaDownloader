package com.reelgrab.domain.usecase

import com.reelgrab.domain.model.DownloadRecord
import com.reelgrab.domain.model.DownloadRequest
import com.reelgrab.domain.model.DownloadStatus
import com.reelgrab.domain.model.MediaItem
import com.reelgrab.domain.repository.DownloadRepository
import com.reelgrab.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Enqueues a download and ensures completed work lands in the persistent history.
 *
 * Why: writing to history is a cross-cutting concern that's easy to forget in a ViewModel.
 * Putting the side-effect here means every entry point (Preview tile, History "retry", a
 * future share-target intent) gets identical bookkeeping. The use case stays a class with
 * `@Inject constructor` per the architecture rules — no top-level functions.
 */
class DownloadMediaUseCase @Inject constructor(
    private val repo: DownloadRepository,
    private val history: HistoryRepository,
) {

    /**
     * Enqueue a download for [item] and stream its [DownloadStatus] back. When the underlying
     * worker reports [DownloadStatus.Completed] the use case writes a [DownloadRecord] into
     * history as a side-effect of collection.
     */
    operator fun invoke(item: MediaItem): Flow<DownloadStatus> {
        val request = DownloadRequest(
            mediaItem = item,
            fileName = sanitize(item.id),
            mimeType = item.type.toMimeType(),
        )
        val workId = repo.enqueue(request)
        return repo.observe(workId).onEach { status ->
            if (status is DownloadStatus.Completed) {
                history.add(
                    DownloadRecord(
                        id = item.id,
                        sourceUrl = item.sourceUrl,
                        savedUri = status.uri,
                        type = item.type,
                        savedAt = System.currentTimeMillis(),
                        sizeBytes = item.sizeBytes ?: 0L,
                    ),
                )
            }
        }
    }

    private fun sanitize(raw: String): String = raw.replace(SANITIZER, "_")

    private fun com.reelgrab.domain.model.MediaType.toMimeType(): String = when (this) {
        com.reelgrab.domain.model.MediaType.VIDEO -> "video/mp4"
        com.reelgrab.domain.model.MediaType.IMAGE -> "image/jpeg"
    }

    private companion object {
        val SANITIZER = Regex("[^A-Za-z0-9._-]")
    }
}
