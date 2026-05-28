package com.reelgrab.data.di

import com.reelgrab.data.download.WorkManagerDownloadRepository
import com.reelgrab.data.extractor.RemoteMediaRepository
import com.reelgrab.data.local.RoomHistoryRepository
import com.reelgrab.data.settings.DataStoreSettingsRepository
import com.reelgrab.domain.repository.DownloadRepository
import com.reelgrab.domain.repository.HistoryRepository
import com.reelgrab.domain.repository.MediaRepository
import com.reelgrab.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Wires every domain repository interface to its concrete `:data` implementation.
 *
 * Why `@Binds` over `@Provides`? The implementations are themselves `@Inject` classes
 * with no construction logic; `@Binds` produces less generated code and forces the
 * compiler to verify all constructor params are reachable.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: RemoteMediaRepository): MediaRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: WorkManagerDownloadRepository): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: RoomHistoryRepository): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
}
