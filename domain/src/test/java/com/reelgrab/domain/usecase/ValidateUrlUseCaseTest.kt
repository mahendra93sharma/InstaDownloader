package com.reelgrab.domain.usecase

import com.reelgrab.domain.model.Source
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ValidateUrlUseCaseTest {

    private val validate = ValidateUrlUseCase()

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://www.instagram.com/p/Cxyz123/",
            "http://instagram.com/p/Cxyz123",
            "https://www.instagram.com/reel/Cxyz-456/",
            "https://www.instagram.com/reel/Cxyz_456/?igshid=abc",
            "https://www.instagram.com/tv/Cxyz789/",
            "https://www.instagram.com/stories/someone/12345",
            "https://instagram.com/reel/abc/",
        ],
    )
    fun `valid instagram urls resolve to INSTAGRAM`(url: String) {
        val result = validate(url)
        assertTrue(result.isSuccess, "Expected success for $url but got ${result.exceptionOrNull()}")
        assertEquals(Source.INSTAGRAM, result.getOrNull())
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://www.facebook.com/watch/?v=1234567890",
            "https://m.facebook.com/video.php?v=1234",
            "https://web.facebook.com/somepage/videos/1234",
            "https://facebook.com/anything",
            "https://fb.watch/abcDEF123/",
            "http://m.facebook.com/anything",
        ],
    )
    fun `valid facebook urls resolve to FACEBOOK`(url: String) {
        val result = validate(url)
        assertTrue(result.isSuccess, "Expected success for $url but got ${result.exceptionOrNull()}")
        assertEquals(Source.FACEBOOK, result.getOrNull())
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://www.tiktok.com/@user/video/12345",
            "https://twitter.com/foo/status/1",
            "https://example.com/p/abc",
            "ftp://instagram.com/p/abc/",
            "instagram.com/p/abc/",
            "https://www.instagram.com/",
            "https://www.instagram.com/explore/",
            "not a url",
        ],
    )
    fun `invalid urls return failure`(url: String) {
        val result = validate(url)
        assertTrue(result.isFailure, "Expected failure for $url")
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `empty string returns failure`() {
        val result = validate("")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `blank string returns failure`() {
        val result = validate("    ")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        val result = validate("  https://www.instagram.com/reel/abc/  ")
        assertTrue(result.isSuccess)
        assertEquals(Source.INSTAGRAM, result.getOrNull())
    }
}
