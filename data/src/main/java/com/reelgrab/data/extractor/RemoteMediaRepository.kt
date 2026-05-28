package com.reelgrab.data.extractor

import com.reelgrab.core.common.dispatcher.AppDispatchers
import com.reelgrab.core.network.api.ExtractionService
import com.reelgrab.core.network.dto.ResolveRequest
import com.reelgrab.core.network.dto.toDomain
import com.reelgrab.domain.model.MediaItem
import com.reelgrab.domain.model.PrivateContentException
import com.reelgrab.domain.model.RateLimitedException
import com.reelgrab.domain.model.UnsupportedSourceException
import com.reelgrab.domain.repository.MediaRepository
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Talks to the extraction microservice and translates transport failures into
 * domain-typed exceptions wrapped in [Result].
 *
 * Why translate at this boundary? Higher layers must not branch on Retrofit /
 * OkHttp types. By rewriting `HttpException(403/429)` and naked `IOException`s
 * into `PrivateContent / RateLimited / Unsupported / Network` here, the domain
 * `FetchMediaUseCase` can map directly to `ErrorReason` with a single `is`-chain.
 *
 * The contract from [MediaRepository] is "never throw, always Result" — we honour
 * it by funnelling everything through `runCatching`. The use case will still see
 * the raw exception type because we don't wrap (only catch).
 */
@Singleton
class RemoteMediaRepository @Inject constructor(
    private val service: ExtractionService,
    private val dispatchers: AppDispatchers,
) : MediaRepository {

    override suspend fun resolve(url: String): Result<List<MediaItem>> =
        withContext(dispatchers.io) {
            runCatching {
                val response = service.resolve(ResolveRequest(url))
                response.items.map { it.toDomain() }
            }.recoverCatching { throwable ->
                throw throwable.translate(url)
            }
        }

    /** Convert transport-layer exceptions into domain-typed exceptions. */
    private fun Throwable.translate(url: String): Throwable {
        // Error-level so the full stacktrace reaches logcat. Previously this was warn-level
        // and the actual cause (e.g. UnknownHostException for the placeholder host) was
        // invisible — the UI showed only the generic "Couldn't reach the server" string.
        Timber.tag("ReelGrab/Extraction").e(
            this,
            "Extraction failed for url=%s (cause=%s)",
            url,
            this::class.java.simpleName,
        )
        return when (this) {
            is HttpException -> when (code()) {
                CODE_FORBIDDEN -> PrivateContentException(cause = this)
                CODE_NOT_FOUND -> UnsupportedSourceException(cause = this)
                CODE_TOO_MANY_REQUESTS -> RateLimitedException(cause = this)
                else -> this
            }
            is IOException -> this // network failure — propagated as-is
            else -> this
        }
    }

    private companion object {
        const val CODE_FORBIDDEN = 403
        const val CODE_NOT_FOUND = 404
        const val CODE_TOO_MANY_REQUESTS = 429
    }
}
