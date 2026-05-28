package com.reelgrab.domain.usecase

import com.reelgrab.domain.model.ErrorReason
import com.reelgrab.domain.model.FetchState
import com.reelgrab.domain.model.MediaItem
import com.reelgrab.domain.model.MediaType
import com.reelgrab.domain.model.PrivateContentException
import com.reelgrab.domain.model.RateLimitedException
import com.reelgrab.domain.model.UnsupportedSourceException
import com.reelgrab.domain.repository.MediaRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class FetchMediaUseCaseTest {

    private val validate = ValidateUrlUseCase()

    private class FakeRepo(
        private val behavior: suspend (String) -> Result<List<MediaItem>>,
    ) : MediaRepository {
        var lastUrl: String? = null
            private set

        override suspend fun resolve(url: String): Result<List<MediaItem>> {
            lastUrl = url
            return behavior(url)
        }
    }

    private fun sampleItem(id: String = "abc") = MediaItem(
        id = id,
        sourceUrl = "https://www.instagram.com/reel/$id/",
        directUrl = "https://cdn/$id.mp4",
        thumbnailUrl = "https://cdn/$id.jpg",
        type = MediaType.VIDEO,
        durationMs = 1500L,
        width = 720,
        height = 1280,
        sizeBytes = 1024L,
    )

    @Test
    fun `happy path returns Success with items`() = runTest {
        val items = listOf(sampleItem())
        val repo = FakeRepo { Result.success(items) }
        val useCase = FetchMediaUseCase(repo, validate)

        val state = useCase("https://www.instagram.com/reel/abc/")

        assertTrue(state is FetchState.Success, "Expected Success but got $state")
        assertEquals(items, (state as FetchState.Success).items)
        assertEquals("https://www.instagram.com/reel/abc/", repo.lastUrl)
    }

    @Test
    fun `invalid url short-circuits to InvalidUrl without hitting repo`() = runTest {
        val repo = FakeRepo { error("repo should not be called") }
        val useCase = FetchMediaUseCase(repo, validate)

        val state = useCase("not a url")

        assertTrue(state is FetchState.Error)
        assertEquals(ErrorReason.InvalidUrl, (state as FetchState.Error).reason)
        assertEquals(null, repo.lastUrl)
    }

    @Test
    fun `IOException from repo maps to Network`() = runTest {
        val repo = FakeRepo { Result.failure(IOException("boom")) }
        val useCase = FetchMediaUseCase(repo, validate)

        val state = useCase("https://www.instagram.com/reel/abc/")

        assertTrue(state is FetchState.Error)
        assertEquals(ErrorReason.Network, (state as FetchState.Error).reason)
    }

    @Test
    fun `repo throwing IOException maps to Network`() = runTest {
        val repo = FakeRepo { throw IOException("conn reset") }
        val useCase = FetchMediaUseCase(repo, validate)

        val state = useCase("https://www.instagram.com/reel/abc/")

        assertTrue(state is FetchState.Error)
        assertEquals(ErrorReason.Network, (state as FetchState.Error).reason)
    }

    @Test
    fun `empty success list maps to Unsupported`() = runTest {
        val repo = FakeRepo { Result.success(emptyList()) }
        val useCase = FetchMediaUseCase(repo, validate)

        val state = useCase("https://www.instagram.com/reel/abc/")

        assertTrue(state is FetchState.Error)
        assertEquals(ErrorReason.Unsupported, (state as FetchState.Error).reason)
    }

    @Test
    fun `PrivateContentException maps to PrivateContent`() = runTest {
        val repo = FakeRepo { Result.failure(PrivateContentException()) }
        val useCase = FetchMediaUseCase(repo, validate)

        val state = useCase("https://www.instagram.com/reel/abc/")

        assertTrue(state is FetchState.Error)
        assertEquals(ErrorReason.PrivateContent, (state as FetchState.Error).reason)
    }

    @Test
    fun `RateLimitedException maps to RateLimited`() = runTest {
        val repo = FakeRepo { Result.failure(RateLimitedException()) }
        val useCase = FetchMediaUseCase(repo, validate)

        val state = useCase("https://www.instagram.com/reel/abc/")

        assertTrue(state is FetchState.Error)
        assertEquals(ErrorReason.RateLimited, (state as FetchState.Error).reason)
    }

    @Test
    fun `UnsupportedSourceException maps to Unsupported`() = runTest {
        val repo = FakeRepo { Result.failure(UnsupportedSourceException()) }
        val useCase = FetchMediaUseCase(repo, validate)

        val state = useCase("https://www.instagram.com/reel/abc/")

        assertTrue(state is FetchState.Error)
        assertEquals(ErrorReason.Unsupported, (state as FetchState.Error).reason)
    }

    @Test
    fun `unknown throwable maps to Unknown with cause`() = runTest {
        val cause = RuntimeException("weird")
        val repo = FakeRepo { Result.failure(cause) }
        val useCase = FetchMediaUseCase(repo, validate)

        val state = useCase("https://www.instagram.com/reel/abc/")

        assertTrue(state is FetchState.Error)
        val reason = (state as FetchState.Error).reason
        assertTrue(reason is ErrorReason.Unknown)
        assertEquals(cause, (reason as ErrorReason.Unknown).cause)
    }
}
