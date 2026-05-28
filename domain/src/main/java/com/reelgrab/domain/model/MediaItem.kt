package com.reelgrab.domain.model

/**
 * Normalized, transport-neutral representation of a single piece of media that can be downloaded.
 *
 * Why: Instagram and Facebook return wildly different payloads (carousels, single posts, reels,
 * stories), and the on-device fallback parses Open Graph tags with yet another shape. Collapsing
 * those into one model means the UI, download worker, and history layer can be written once and
 * stay platform-agnostic. The class deliberately contains no Android types so the domain module
 * remains pure JVM and easy to unit-test.
 *
 * - [id] stable identifier for diffing in lazy lists and as a Work tag.
 * - [sourceUrl] the original page URL the user pasted (kept for history + re-share).
 * - [directUrl] the direct CDN URL the download worker streams from.
 * - [thumbnailUrl] used by Coil in the preview grid.
 * - [durationMs] populated for [MediaType.VIDEO] when the extractor knows it; null otherwise.
 * - [sizeBytes] best-effort size used to render progress; null when the server omits it.
 */
data class MediaItem(
    val id: String,
    val sourceUrl: String,
    val directUrl: String,
    val thumbnailUrl: String,
    val type: MediaType,
    val durationMs: Long? = null,
    val width: Int,
    val height: Int,
    val sizeBytes: Long? = null,
)
