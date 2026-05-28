package com.reelgrab.data.download

/**
 * Centralized WorkManager + notification constants used by [DownloadWorker] and
 * [WorkManagerDownloadRepository].
 *
 * Why a constants file? The keys are passed between the repository (producer)
 * and the worker (consumer) — keeping them as `const val`s prevents typos and
 * makes refactors safe via "find usages".
 */
object DownloadConstants {

    // Input data keys
    const val KEY_URL = "url"
    const val KEY_FILE_NAME = "fileName"
    const val KEY_MIME_TYPE = "mimeType"
    const val KEY_ITEM_ID = "itemId"
    const val KEY_SOURCE_URL = "sourceUrl"
    const val KEY_MEDIA_TYPE = "mediaType" // "VIDEO" or "IMAGE"

    // Output data keys
    const val KEY_PROGRESS = "progress"
    const val KEY_OUTPUT_URI = "uri"
    const val KEY_FAILURE_REASON = "reason"

    // Notification + WorkManager tagging
    const val NOTIFICATION_CHANNEL_ID = "reelgrab.downloads"
    const val NOTIFICATION_CHANNEL_NAME = "Downloads"
    const val NOTIFICATION_ID_BASE = 1_000

    /** Prefix for tagging work requests so the History screen can query them all. */
    const val TAG_PREFIX = "reelgrab.download:"
    const val TAG_ALL = "reelgrab.download"

    // MediaStore subdirectory names
    const val IMAGES_RELATIVE_DIR = "Pictures/ReelGrab"
    const val VIDEOS_RELATIVE_DIR = "Movies/ReelGrab"

    // Failure tags returned via output data
    const val FAILURE_NETWORK = "network"
    const val FAILURE_PRIVATE = "private"
    const val FAILURE_RATE_LIMITED = "rate_limited"
    const val FAILURE_IO = "io"
    const val FAILURE_UNKNOWN = "unknown"
}
