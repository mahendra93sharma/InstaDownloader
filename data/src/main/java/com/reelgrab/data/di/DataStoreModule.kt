package com.reelgrab.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Provides a single [DataStore]<[Preferences]> instance for app settings.
 *
 * Why hand-rolled instead of `Context.dataStore` delegate? The delegate is
 * file-scope and harder to inject into ViewModels in test runs. Building the
 * factory ourselves keeps it Hilt-friendly and lets us pin the scope to a
 * `SupervisorJob` so a single write failure doesn't tear down all observers.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    private const val PREFS_FILE = "reelgrab_settings"

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { context.preferencesDataStoreFile(PREFS_FILE) },
    )
}
