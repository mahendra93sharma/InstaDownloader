package com.reelgrab.core.network.dto

import com.reelgrab.domain.model.MediaItem
import com.reelgrab.domain.model.MediaType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire-format DTOs for the extraction microservice.
 *
 * Why a separate DTO layer? The domain [MediaItem] should remain stable even if the
 * server adds fields or changes naming conventions; keeping serialization concerns
 * (`@SerialName`, JSON-only `type` enum) out of the domain class isolates that
 * churn. `ignoreUnknownKeys = true` in the Json provider lets the server add
 * fields without breaking older clients.
 */
@Serializable
data class ResolveRequest(val url: String)

@Serializable
data class ResolveResponse(val items: List<MediaDto>)

@Serializable
data class MediaDto(
    val id: String,
    val sourceUrl: String,
    val directUrl: String,
    // Defaulted so the client survives extractor backends that omit a field — yt-dlp,
    // for instance, occasionally returns no width/height on Stories. Server-side we
    // coerce these to 0 / fall back to directUrl, but defaults here are defense in
    // depth against a future backend skew.
    val thumbnailUrl: String = "",
    val type: MediaTypeDto,
    val durationMs: Long? = null,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long? = null,
)

@Serializable
enum class MediaTypeDto {
    @SerialName("VIDEO") VIDEO,
    @SerialName("IMAGE") IMAGE,
}

/** Map server payload into the domain model used by the rest of the app. */
fun MediaDto.toDomain(): MediaItem = MediaItem(
    id = id,
    sourceUrl = sourceUrl,
    directUrl = directUrl,
    thumbnailUrl = thumbnailUrl,
    type = when (type) {
        MediaTypeDto.VIDEO -> MediaType.VIDEO
        MediaTypeDto.IMAGE -> MediaType.IMAGE
    },
    durationMs = durationMs,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
)
