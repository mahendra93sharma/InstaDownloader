package com.reelgrab.data.extractor

import com.reelgrab.core.common.dispatcher.AppDispatchers
import com.reelgrab.core.network.api.ExtractionService
import com.reelgrab.core.network.dto.MediaDto
import com.reelgrab.core.network.dto.MediaTypeDto
import com.reelgrab.core.network.dto.ResolveResponse
import com.reelgrab.domain.model.PrivateContentException
import com.reelgrab.domain.model.RateLimitedException
import com.reelgrab.domain.model.UnsupportedSourceException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteMediaRepositoryTest {

    private val service: ExtractionService = mockk()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : AppDispatchers {
        override val io = testDispatcher
        override val default = testDispatcher
        override val main = testDispatcher
    }

    private fun repo() = RemoteMediaRepository(service, dispatchers)

    private fun httpException(code: Int): HttpException {
        val response = Response.error<Any>(
            code,
            "".toResponseBody("application/json".toMediaType()),
        )
        return HttpException(response)
    }

    @Test
    fun `HTTP 403 maps to PrivateContentException in Result`() = runTest(testDispatcher) {
        coEvery { service.resolve(any()) } throws httpException(403)

        val result = repo().resolve("https://www.instagram.com/reel/abc/")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PrivateContentException)
    }

    @Test
    fun `HTTP 429 maps to RateLimitedException in Result`() = runTest(testDispatcher) {
        coEvery { service.resolve(any()) } throws httpException(429)

        val result = repo().resolve("https://www.instagram.com/reel/abc/")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RateLimitedException)
    }

    @Test
    fun `HTTP 404 maps to UnsupportedSourceException in Result`() = runTest(testDispatcher) {
        coEvery { service.resolve(any()) } throws httpException(404)

        val result = repo().resolve("https://www.instagram.com/reel/abc/")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedSourceException)
    }

    @Test
    fun `IOException propagates unchanged`() = runTest(testDispatcher) {
        val boom = IOException("conn reset")
        coEvery { service.resolve(any()) } throws boom

        val result = repo().resolve("https://www.instagram.com/reel/abc/")

        assertTrue(result.isFailure)
        val cause = result.exceptionOrNull()
        assertTrue(cause is IOException)
        assertEquals("conn reset", cause?.message)
    }

    @Test
    fun `happy path returns mapped MediaItem list`() = runTest(testDispatcher) {
        coEvery { service.resolve(any()) } returns ResolveResponse(
            items = listOf(
                MediaDto(
                    id = "abc",
                    sourceUrl = "https://www.instagram.com/reel/abc/",
                    directUrl = "https://cdn/abc.mp4",
                    thumbnailUrl = "https://cdn/abc.jpg",
                    type = MediaTypeDto.VIDEO,
                    durationMs = 1500L,
                    width = 720,
                    height = 1280,
                    sizeBytes = 1024L,
                ),
            ),
        )

        val result = repo().resolve("https://www.instagram.com/reel/abc/")

        assertTrue(result.isSuccess)
        val items = result.getOrThrow()
        assertEquals(1, items.size)
        assertEquals("abc", items.first().id)
        assertEquals("https://cdn/abc.mp4", items.first().directUrl)
    }
}
