package com.reelgrab.data.download

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.reelgrab.data.download.DownloadConstants.KEY_FAILURE_REASON
import com.reelgrab.data.download.DownloadConstants.KEY_FILE_NAME
import com.reelgrab.data.download.DownloadConstants.KEY_ITEM_ID
import com.reelgrab.data.download.DownloadConstants.KEY_MEDIA_TYPE
import com.reelgrab.data.download.DownloadConstants.KEY_MIME_TYPE
import com.reelgrab.data.download.DownloadConstants.KEY_OUTPUT_URI
import com.reelgrab.data.download.DownloadConstants.KEY_PROGRESS
import com.reelgrab.data.download.DownloadConstants.KEY_SOURCE_URL
import com.reelgrab.data.download.DownloadConstants.KEY_URL
import com.reelgrab.data.download.DownloadConstants.TAG_ALL
import com.reelgrab.data.download.DownloadConstants.TAG_PREFIX
import com.reelgrab.domain.model.DownloadRequest
import com.reelgrab.domain.model.DownloadStatus
import com.reelgrab.domain.model.ErrorReason
import com.reelgrab.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkManager-backed [DownloadRepository] implementation.
 *
 * Why both per-id and per-tag observation? The Preview screen wants a single
 * `Flow<DownloadStatus>` for the item the user just tapped, while the History
 * screen wants every in-flight + recently-finished work. WorkManager exposes
 * both via `getWorkInfoByIdFlow` and `getWorkInfosByTagFlow` (in the AndroidX
 * Work 2.9 `-ktx` artifact).
 *
 * The item id is encoded into the work tag (`TAG_PREFIX + itemId`) so we can
 * always recover it from a `WorkInfo` without a parallel index.
 */
@Singleton
class WorkManagerDownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : DownloadRepository {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    override fun enqueue(request: DownloadRequest): String {
        val item = request.mediaItem
        val data = workDataOf(
            KEY_URL to item.directUrl,
            KEY_FILE_NAME to request.fileName,
            KEY_MIME_TYPE to request.mimeType,
            KEY_ITEM_ID to item.id,
            KEY_SOURCE_URL to item.sourceUrl,
            KEY_MEDIA_TYPE to item.type.name,
        )
        val work = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .addTag(TAG_ALL)
            .addTag(TAG_PREFIX + item.id)
            .build()
        workManager.enqueue(work)
        return work.id.toString()
    }

    override fun observe(workId: String): Flow<DownloadStatus> =
        workManager.getWorkInfoByIdFlow(java.util.UUID.fromString(workId))
            .map { info -> info.toDomain() }

    override fun observeAll(): Flow<List<DownloadStatus>> =
        workManager.getWorkInfosByTagFlow(TAG_ALL)
            .map { list -> list.map { it.toDomain() } }

    /** Translate a [WorkInfo] snapshot into our domain status. */
    private fun WorkInfo?.toDomain(): DownloadStatus {
        if (this == null) return DownloadStatus.Failed("", ErrorReason.Unknown())
        val itemId = tags.firstOrNull { it.startsWith(TAG_PREFIX) }
            ?.removePrefix(TAG_PREFIX)
            .orEmpty()
        val progress = progress.getInt(KEY_PROGRESS, 0)
        return when (state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadStatus.Queued(itemId)
            WorkInfo.State.RUNNING -> DownloadStatus.Running(itemId, progress)
            WorkInfo.State.SUCCEEDED -> {
                val uri = outputData.getString(KEY_OUTPUT_URI).orEmpty()
                DownloadStatus.Completed(itemId, uri)
            }
            WorkInfo.State.FAILED -> DownloadStatus.Failed(
                itemId,
                mapFailure(outputData.getString(KEY_FAILURE_REASON)),
            )
            WorkInfo.State.CANCELLED -> DownloadStatus.Failed(itemId, ErrorReason.Unknown())
        }
    }

    private fun mapFailure(tag: String?): ErrorReason = when (tag) {
        DownloadConstants.FAILURE_NETWORK -> ErrorReason.Network
        DownloadConstants.FAILURE_PRIVATE -> ErrorReason.PrivateContent
        DownloadConstants.FAILURE_RATE_LIMITED -> ErrorReason.RateLimited
        DownloadConstants.FAILURE_IO -> ErrorReason.Network
        else -> ErrorReason.Unknown()
    }
}
