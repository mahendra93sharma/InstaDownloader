package com.reelgrab.core.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction over [kotlinx.coroutines.Dispatchers] so tests can inject a
 * `TestDispatcher` while production code uses real IO / Default / Main pools.
 *
 * Why: hard-coding `Dispatchers.IO` inside repositories makes them painful to
 * unit-test (you can't pin virtual time). The interface stays in `:core:common`
 * so every layer can depend on it; the Hilt binding to [DefaultDispatchers]
 * lives in `:data/di/DispatcherModule.kt`.
 */
interface AppDispatchers {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}

/** Production binding that delegates straight to the kotlinx-coroutines pools. */
@Singleton
class DefaultDispatchers @Inject constructor() : AppDispatchers {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val main: CoroutineDispatcher = Dispatchers.Main
}
