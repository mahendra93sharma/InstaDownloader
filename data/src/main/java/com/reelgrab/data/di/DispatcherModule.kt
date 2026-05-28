package com.reelgrab.data.di

import com.reelgrab.core.common.dispatcher.AppDispatchers
import com.reelgrab.core.common.dispatcher.DefaultDispatchers
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the production [DefaultDispatchers] for every [AppDispatchers] injection.
 *
 * Why a separate binding module? Tests can `@TestInstallIn` replace this with a
 * `TestDispatchers` impl that returns `StandardTestDispatcher`, without touching
 * the rest of the graph.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DispatcherModule {

    @Binds
    @Singleton
    abstract fun bindAppDispatchers(impl: DefaultDispatchers): AppDispatchers
}
