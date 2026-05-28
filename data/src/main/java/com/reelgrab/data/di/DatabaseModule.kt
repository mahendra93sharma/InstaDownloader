package com.reelgrab.data.di

import android.content.Context
import androidx.room.Room
import com.reelgrab.data.local.DownloadDao
import com.reelgrab.data.local.ReelGrabDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-managed Room database and DAOs.
 *
 * Why a singleton DB? Room handles concurrency internally but opening multiple
 * SQLite handles wastes file descriptors and lets writes race across instances.
 * We disable schema export (v1, no migrations yet) — re-enable before shipping a
 * v2 schema so the JSON snapshots end up in version control.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DB_NAME = "reelgrab.db"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ReelGrabDatabase =
        Room.databaseBuilder(context, ReelGrabDatabase::class.java, DB_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDownloadDao(db: ReelGrabDatabase): DownloadDao = db.downloadDao()
}
