package com.reelgrab.data.di

import com.reelgrab.core.network.api.ExtractionService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

/**
 * Provides Retrofit-backed services that live in `:data`.
 *
 * Why here and not in `:core:network`? Service interfaces are wire contracts that
 * belong with the network module, but Hilt prefers binding services in the same
 * graph that uses them — keeps churn local when a new endpoint is added.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideExtractionService(retrofit: Retrofit): ExtractionService = retrofit.create()
}
