package com.reelgrab.domain.model

/**
 * Transport-neutral payload that the download repository turns into a WorkManager request.
 *
 * Why: keeping this a plain Kotlin data class (no `Data`, `Uri`, or `Context`) lets the domain
 * use-cases construct it without depending on Android. The data layer is solely responsible for
 * converting it into a [androidx.work.Data] bundle and MediaStore entry — see spec §8.
 *
 * - [fileName] should already be sanitized by the caller using `Regex("[^A-Za-z0-9._-]")`.
 * - [mimeType] feeds MediaStore so the OS displays the right thumbnail and Photos category.
 */
data class DownloadRequest(
    val mediaItem: MediaItem,
    val fileName: String,
    val mimeType: String,
)
