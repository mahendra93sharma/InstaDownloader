package com.reelgrab.core.network.api

import com.reelgrab.core.network.dto.ResolveRequest
import com.reelgrab.core.network.dto.ResolveResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit binding for the extraction microservice (spec §7).
 *
 * Why server-side resolution? Instagram and Facebook rotate their HTML / GraphQL
 * payloads frequently. Keeping extraction off-device lets us ship hotfixes server
 * side without an app release. The contract is intentionally tiny — one POST in,
 * a normalized list of MediaItems out.
 */
interface ExtractionService {

    /** Resolve a public post / reel / Story / Facebook video URL into downloadable media. */
    @POST("resolve")
    suspend fun resolve(@Body request: ResolveRequest): ResolveResponse
}
