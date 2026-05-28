package com.reelgrab.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module providing the singleton HTTP stack the rest of the app talks through.
 *
 * Why one module? OkHttp / Retrofit / Json are intertwined (Retrofit needs OkHttp +
 * the JSON converter, the converter needs Json) and pre-warming the OkHttp connection
 * pool at app start (spec §11) demands a single instance. Splitting these into
 * separate modules creates ordering ambiguity and forces multiple factories.
 *
 * The 30-second timeouts match the extraction service SLA; raise this only after
 * profiling — long timeouts mask backend regressions during development.
 */
@Module
@InstallIn(SingletonComponent::class)
object OkHttpModule {

    // Render free/starter spins up cold (~30-60s). Connect timeout stays
    // short (TCP is fast); read timeout absorbs cold start + yt-dlp work.
    private const val CONNECT_TIMEOUT_SECONDS = 20L
    private const val READ_TIMEOUT_SECONDS = 90L
    private const val WRITE_TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = when {
                BuildConfig.ENABLE_HTTP_BODY_LOGGING -> HttpLoggingInterceptor.Level.BODY
                BuildConfig.ENABLE_HTTP_LOGGING -> HttpLoggingInterceptor.Level.BASIC
                else -> HttpLoggingInterceptor.Level.NONE
            }
        }

    /**
     * Placeholder pinner. We must pin the extraction service's leaf + intermediate
     * SPKI hashes before shipping, but the production host doesn't exist yet, so
     * pinning a fake fingerprint would break every debug build. Returning
     * [CertificatePinner.DEFAULT] keeps the wiring in place — swap in real pins
     * once the host is locked in.
     *
     * TODO(reelgrab/security#1): pin certificates once extraction service host is known.
     */
    @Provides
    @Singleton
    fun provideCertificatePinner(): CertificatePinner = CertificatePinner.DEFAULT

    @Provides
    @Singleton
    fun provideOkHttpClient(
        logging: HttpLoggingInterceptor,
        pinner: CertificatePinner,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .certificatePinner(pinner)
            .retryOnConnectionFailure(true)

        val apiKey = BuildConfig.EXTRACTION_API_KEY
        if (apiKey.isNotEmpty()) {
            builder.addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("X-API-Key", apiKey)
                    .build()
                chain.proceed(req)
            }
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttp: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.EXTRACTION_BASE_URL)
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
