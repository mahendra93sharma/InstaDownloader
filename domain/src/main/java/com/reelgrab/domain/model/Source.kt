package com.reelgrab.domain.model

/**
 * Identifies which upstream platform a pasted URL belongs to.
 *
 * Why: the result of [com.reelgrab.domain.usecase.ValidateUrlUseCase] is consumed by the data
 * layer to pick the right extractor (Instagram vs Facebook). Encoding the platform here, rather
 * than re-running regex downstream, keeps the validation logic in exactly one place.
 */
enum class Source {
    INSTAGRAM,
    FACEBOOK,
}
