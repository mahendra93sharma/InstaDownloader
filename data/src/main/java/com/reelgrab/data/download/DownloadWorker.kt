package com.reelgrab.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.reelgrab.core.common.extensions.sanitizeFileName
import com.reelgrab.data.download.DownloadConstants.FAILURE_IO
import com.reelgrab.data.download.DownloadConstants.FAILURE_NETWORK
import com.reelgrab.data.download.DownloadConstants.FAILURE_UNKNOWN
import com.reelgrab.data.download.DownloadConstants.IMAGES_RELATIVE_DIR
import com.reelgrab.data.download.DownloadConstants.KEY_FAILURE_REASON
import com.reelgrab.data.download.DownloadConstants.KEY_FILE_NAME
import com.reelgrab.data.download.DownloadConstants.KEY_ITEM_ID
import com.reelgrab.data.download.DownloadConstants.KEY_MEDIA_TYPE
import com.reelgrab.data.download.DownloadConstants.KEY_MIME_TYPE
import com.reelgrab.data.download.DownloadConstants.KEY_OUTPUT_URI
import com.reelgrab.data.download.DownloadConstants.KEY_PROGRESS
import com.reelgrab.data.download.DownloadConstants.KEY_SOURCE_URL
import com.reelgrab.data.download.DownloadConstants.KEY_URL
import com.reelgrab.data.download.DownloadConstants.NOTIFICATION_CHANNEL_ID
import com.reelgrab.data.download.DownloadConstants.NOTIFICATION_CHANNEL_NAME
import com.reelgrab.data.download.DownloadConstants.NOTIFICATION_ID_BASE
import com.reelgrab.data.download.DownloadConstants.VIDEOS_RELATIVE_DIR
import com.reelgrab.domain.model.DownloadRecord
import com.reelgrab.domain.model.MediaType
import com.reelgrab.domain.repository.HistoryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException

/**
 * Streams a single media file from the extractor CDN into the user's MediaStore.
 *
 * Why a CoroutineWorker? WorkManager guarantees the job survives process death and
 * battery-saver restrictions, while the coroutine API lets us treat the OkHttp
 * stream + `setProgress` as suspending calls. Streaming straight from
 * `ResponseBody.byteStream()` into `ContentResolver.openOutputStream()` keeps the
 * peak memory tiny even for hundred-megabyte videos.
 *
 * The `IS_PENDING` flag prevents the Photos app and the MediaScanner from picking
 * up half-written files; we clear it only after the stream copies cleanly. On
 * failure the partial row is deleted so the gallery never shows broken entries.
 *
 * On Android 13+ a notification permission may be required for the foreground
 * service notification to render. The `:app` module requests POST_NOTIFICATIONS;
 * if the user declines, the worker still runs (Android downgrades the foreground
 * service silently).
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val historyRepo: HistoryRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return failure(FAILURE_UNKNOWN)
        val rawFileName = inputData.getString(KEY_FILE_NAME) ?: return failure(FAILURE_UNKNOWN)
        val mimeType = inputData.getString(KEY_MIME_TYPE) ?: return failure(FAILURE_UNKNOWN)
        val itemId = inputData.getString(KEY_ITEM_ID) ?: return failure(FAILURE_UNKNOWN)
        val sourceUrl = inputData.getString(KEY_SOURCE_URL) ?: return failure(FAILURE_UNKNOWN)
        val mediaTypeRaw = inputData.getString(KEY_MEDIA_TYPE) ?: return failure(FAILURE_UNKNOWN)
        val mediaType = runCatching { MediaType.valueOf(mediaTypeRaw) }
            .getOrElse { return failure(FAILURE_UNKNOWN) }

        val fileName = rawFileName.sanitizeFileName()

        ensureChannel()
        setForeground(buildForegroundInfo(progress = 0, fileName = fileName))

        var insertedUri: Uri? = null
        return try {
            insertedUri = insertPending(fileName, mimeType, mediaType)
                ?: return failure(FAILURE_IO)

            val totalBytes = streamInto(insertedUri, url)
            clearPending(insertedUri)
            applicationContext.contentResolver.notifyChange(insertedUri, null)

            historyRepo.add(
                DownloadRecord(
                    id = itemId,
                    sourceUrl = sourceUrl,
                    savedUri = insertedUri.toString(),
                    type = mediaType,
                    savedAt = System.currentTimeMillis(),
                    sizeBytes = totalBytes,
                ),
            )

            Result.success(workDataOf(KEY_OUTPUT_URI to insertedUri.toString()))
        } catch (io: IOException) {
            Timber.e(io, "Download failed for %s", url)
            insertedUri?.let { applicationContext.contentResolver.delete(it, null, null) }
            failure(FAILURE_NETWORK)
        } catch (t: Throwable) {
            Timber.e(t, "Unexpected download failure for %s", url)
            insertedUri?.let { applicationContext.contentResolver.delete(it, null, null) }
            failure(FAILURE_UNKNOWN)
        }
    }

    /** Insert a pending MediaStore row in the correct collection for the media kind. */
    private fun insertPending(fileName: String, mimeType: String, mediaType: MediaType): Uri? {
        val (collection, relativeDir) = when (mediaType) {
            MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI to VIDEOS_RELATIVE_DIR
            MediaType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI to IMAGES_RELATIVE_DIR
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        return applicationContext.contentResolver.insert(collection, values)
    }

    /** Stream bytes from [url] into [target], emitting progress along the way. */
    @Suppress("ThrowsCount", "NestedBlockDepth")
    private suspend fun streamInto(target: Uri, url: String): Long {
        val request = Request.Builder().url(url).build()
        val call = okHttpClient.newCall(request)
        call.execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty body")
            val totalBytes = body.contentLength().takeIf { it > 0 } ?: -1L

            val outputStream = applicationContext.contentResolver.openOutputStream(target)
                ?: throw IOException("No output stream for $target")

            val buffer = ByteArray(BUFFER_SIZE)
            var copied = 0L
            var lastPercent = -1
            outputStream.use { out ->
                body.byteStream().use { input ->
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        copied += read
                        if (totalBytes > 0) {
                            val pct = ((copied * 100) / totalBytes).toInt().coerceIn(0, 100)
                            if (pct != lastPercent) {
                                lastPercent = pct
                                setProgress(workDataOf(KEY_PROGRESS to pct))
                            }
                        }
                    }
                }
            }
            return copied
        }
    }

    private fun clearPending(uri: Uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        applicationContext.contentResolver.update(uri, values, null, null)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    private fun buildForegroundInfo(progress: Int, fileName: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Downloading $fileName")
            .setContentText("$progress%")
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .build()
        val notificationId = NOTIFICATION_ID_BASE + (id.hashCode() and 0xFFFF)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun failure(tag: String): Result =
        Result.failure(workDataOf(KEY_FAILURE_REASON to tag))

    private companion object {
        const val BUFFER_SIZE = 8 * 1024
    }
}
