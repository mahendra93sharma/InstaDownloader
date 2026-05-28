package com.reelgrab.domain.usecase

import com.reelgrab.domain.model.Source
import javax.inject.Inject

/**
 * Pure, synchronous URL classifier that gates every other action in the app.
 *
 * Why: validation lives in exactly one place so the regexes in the spec (§3) cannot drift
 * between Home, Preview, and any deep-link handler we add later. The use case returns a
 * [Result] so callers can `getOrElse` without needing a wrapper sealed class — failure is
 * always the same `IllegalArgumentException`, which the ViewModel maps to
 * [com.reelgrab.domain.model.ErrorReason.InvalidUrl].
 */
class ValidateUrlUseCase @Inject constructor() {

    /** Returns the [Source] the URL points at, or a failure when neither regex matches. */
    operator fun invoke(url: String): Result<Source> {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("URL is empty"))
        }
        return when {
            INSTAGRAM.matches(trimmed) -> Result.success(Source.INSTAGRAM)
            FACEBOOK.matches(trimmed) -> Result.success(Source.FACEBOOK)
            else -> Result.failure(IllegalArgumentException("URL does not match Instagram or Facebook patterns"))
        }
    }

    companion object {
        /** Canonical Instagram post / reel / IGTV / story URL pattern (spec §3). */
        val INSTAGRAM = Regex("""^https?://(www\.)?instagram\.com/(p|reel|tv|stories)/[\w\-]+/?.*$""")

        /** Canonical Facebook video URL pattern, including m. / web. subdomains and fb.watch. */
        val FACEBOOK = Regex("""^https?://(www\.|m\.|web\.)?(facebook\.com|fb\.watch)/.*$""")
    }
}
