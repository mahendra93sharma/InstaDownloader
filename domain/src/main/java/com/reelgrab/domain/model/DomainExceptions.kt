package com.reelgrab.domain.model

import java.io.IOException

/**
 * Domain-level extraction failures.
 *
 * Why: keeping these in `:domain` lets `FetchMediaUseCase` translate them into
 * [ErrorReason] cases without importing `:data`. Concrete repositories in `:data`
 * throw / wrap these to convey *semantic* failures (private content, rate limit,
 * unsupported host) that aren't already covered by plain [IOException] / network
 * faults. They extend [IOException] so existing IO-aware error handling still
 * treats them as transport-related, but allows precise `is`-matching for richer
 * UX (e.g. a dedicated "private content" message).
 *
 * Pure JVM — no Android imports — so the domain module remains a plain Kotlin
 * library suitable for offline unit tests.
 */
open class PrivateContentException(
    message: String? = "Content is private or login-gated",
    cause: Throwable? = null,
) : IOException(message, cause)

open class RateLimitedException(
    message: String? = "Extraction backend rate-limited the request",
    cause: Throwable? = null,
) : IOException(message, cause)

open class UnsupportedSourceException(
    message: String? = "URL host or path is not supported by the extractor",
    cause: Throwable? = null,
) : IOException(message, cause)
