package com.reelgrab.domain.repository

import com.reelgrab.domain.model.MediaItem

/**
 * Boundary between the domain layer and the extraction subsystem.
 *
 * Why: the actual extractor may be a remote microservice (preferred), an on-device OG-tag
 * parser (fallback), or a fake in tests. The use case only depends on this interface so
 * swapping implementations stays a wiring concern handled by Hilt in `:data`.
 */
interface MediaRepository {
    /**
     * Resolve a public post / reel / video URL into one or more downloadable items.
     *
     * Implementations MUST return [Result.failure] rather than throw — callers map the wrapped
     * throwable into a [com.reelgrab.domain.model.ErrorReason] for the UI.
     */
    suspend fun resolve(url: String): Result<List<MediaItem>>
}
