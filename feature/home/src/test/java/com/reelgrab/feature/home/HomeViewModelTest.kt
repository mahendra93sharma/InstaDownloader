package com.reelgrab.feature.home

import app.cash.turbine.test
import com.reelgrab.domain.model.ErrorReason
import com.reelgrab.domain.model.FetchState
import com.reelgrab.domain.model.MediaItem
import com.reelgrab.domain.model.MediaType
import com.reelgrab.domain.model.Source
import com.reelgrab.domain.usecase.FetchMediaUseCase
import com.reelgrab.domain.usecase.ValidateUrlUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherRule()

    private val validate = ValidateUrlUseCase()
    private val fetchMedia: FetchMediaUseCase = mockk()

    private fun viewModel() = HomeViewModel(validate, fetchMedia)

    private fun item() = MediaItem(
        id = "abc",
        sourceUrl = "https://www.instagram.com/reel/abc/",
        directUrl = "https://cdn/abc.mp4",
        thumbnailUrl = "https://cdn/abc.jpg",
        type = MediaType.VIDEO,
        width = 720,
        height = 1280,
    )

    @Test
    fun `initial state is empty url with invalid flag`() {
        val vm = viewModel()
        val state = vm.state.value
        assertEquals("", state.url)
        assertFalse(state.urlValid)
        assertNull(state.source)
        assertFalse(state.isLoading)
        assertNull(state.fetchError)
    }

    @Test
    fun `OnUrlChange with valid Instagram URL flips urlValid and sets source`() {
        val vm = viewModel()
        vm.onEvent(HomeEvent.OnUrlChange("https://www.instagram.com/reel/abc/"))
        val state = vm.state.value
        assertTrue(state.urlValid)
        assertEquals(Source.INSTAGRAM, state.source)
    }

    @Test
    fun `OnUrlChange with valid Facebook URL flips urlValid and sets source`() {
        val vm = viewModel()
        vm.onEvent(HomeEvent.OnUrlChange("https://www.facebook.com/watch/?v=12345"))
        val state = vm.state.value
        assertTrue(state.urlValid)
        assertEquals(Source.FACEBOOK, state.source)
    }

    @Test
    fun `OnUrlChange with garbage clears valid flag`() {
        val vm = viewModel()
        vm.onEvent(HomeEvent.OnUrlChange("not-a-url"))
        val state = vm.state.value
        assertFalse(state.urlValid)
        assertNull(state.source)
    }

    @Test
    fun `OnClear resets all url fields`() {
        val vm = viewModel()
        vm.onEvent(HomeEvent.OnUrlChange("https://www.instagram.com/reel/abc/"))
        vm.onEvent(HomeEvent.OnClear)
        val state = vm.state.value
        assertEquals("", state.url)
        assertFalse(state.urlValid)
        assertNull(state.source)
        assertNull(state.fetchError)
    }

    @Test
    fun `OnSubmit valid url + Success emits NavigateToPreview`() = runTest(mainDispatcher.testDispatcher) {
        coEvery { fetchMedia(any()) } returns FetchState.Success(listOf(item()))
        val vm = viewModel()
        vm.onEvent(HomeEvent.OnUrlChange("https://www.instagram.com/reel/abc/"))

        vm.effects.test {
            vm.onEvent(HomeEvent.OnSubmit)
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is HomeEffect.NavigateToPreview)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnSubmit valid url + Error sets fetchError and emits no nav`() = runTest(mainDispatcher.testDispatcher) {
        coEvery { fetchMedia(any()) } returns FetchState.Error(ErrorReason.Network)
        val vm = viewModel()
        vm.onEvent(HomeEvent.OnUrlChange("https://www.instagram.com/reel/abc/"))

        vm.onEvent(HomeEvent.OnSubmit)
        advanceUntilIdle()

        assertEquals(ErrorReason.Network, vm.state.value.fetchError)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `OnSubmit with invalid url emits ShowSnackbar`() = runTest(mainDispatcher.testDispatcher) {
        val vm = viewModel()

        vm.effects.test {
            vm.onEvent(HomeEvent.OnSubmit)
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is HomeEffect.ShowSnackbar)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
