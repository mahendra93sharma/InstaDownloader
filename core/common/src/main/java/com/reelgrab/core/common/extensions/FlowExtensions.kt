package com.reelgrab.core.common.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Wraps each emission of `this` in [Result.success] and any thrown exception in
 * [Result.failure], terminating the upstream cleanly.
 *
 * Why: upstream Flows from Retrofit / Room can throw arbitrary checked / unchecked
 * exceptions. Funnelling them through a Result-typed downstream means ViewModels
 * can collect a single homogeneous stream instead of wrapping every `collect` in
 * a try/catch — see the data-binding pattern recommended in the Now-In-Android
 * sample.
 */
fun <T> Flow<T>.asResult(): Flow<Result<T>> = this
    .map { Result.success(it) }
    .catch { emit(Result.failure(it)) }
