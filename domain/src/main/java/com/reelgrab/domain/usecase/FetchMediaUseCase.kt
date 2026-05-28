package com.reelgrab.domain.usecase

import com.reelgrab.domain.model.ErrorReason
import com.reelgrab.domain.model.FetchState
import com.reelgrab.domain.model.PrivateContentException
import com.reelgrab.domain.model.RateLimitedException
import com.reelgrab.domain.model.UnsupportedSourceException
import com.reelgrab.domain.repository.MediaRepository
import java.io.IOException
import javax.inject.Inject

/**
 * Orchestrates URL validation + media extraction into a single [FetchState] the UI can render.
 *
 * Why: the Home / Preview ViewModels should not branch on `Result<...>` and exception types
 * themselves — they should `collect` a stream of [FetchState] and switch on the sealed type.
 * This use case centralizes the validation -> resolve -> error-mapping pipeline so that policy
 * (e.g. which throwable becomes which [ErrorReason]) lives in one place.
 */
class FetchMediaUseCase @Inject constructor(
    private val repo: MediaRepository,
    private val validate: ValidateUrlUseCase,
) {

    suspend operator fun invoke(url: String): FetchState {
        validate(url).getOrElse {
            return FetchState.Error(ErrorReason.InvalidUrl)
        }

        return runCatching { repo.resolve(url) }
            .fold(
                onSuccess = { result ->
                    result.fold(
                        onSuccess = { items ->
                            if (items.isEmpty()) {
                                FetchState.Error(ErrorReason.Unsupported)
                            } else {
                                FetchState.Success(items)
                            }
                        },
                        onFailure = { throwable -> FetchState.Error(throwable.toErrorReason()) },
                    )
                },
                onFailure = { throwable -> FetchState.Error(throwable.toErrorReason()) },
            )
    }

    private fun Throwable.toErrorReason(): ErrorReason = when (this) {
        is PrivateContentException -> ErrorReason.PrivateContent
        is RateLimitedException -> ErrorReason.RateLimited
        is UnsupportedSourceException -> ErrorReason.Unsupported
        is IOException -> ErrorReason.Network
        is IllegalArgumentException -> ErrorReason.InvalidUrl
        else -> ErrorReason.Unknown(this)
    }
}
