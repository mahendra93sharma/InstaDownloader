package com.reelgrab.feature.preview

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.reelgrab.domain.model.DownloadStatus
import com.reelgrab.domain.model.ErrorReason
import com.reelgrab.domain.model.FetchState
import com.reelgrab.domain.model.MediaItem
import com.reelgrab.domain.model.MediaType
import com.reelgrab.domain.usecase.DownloadMediaUseCase
import com.reelgrab.domain.usecase.FetchMediaUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.net.URLEncoder

@OptIn(ExperimentalCoroutinesApi::class)
class PreviewViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherRule()

    private val fetch: FetchMediaUseCase = mockk()
    private val download: DownloadMediaUseCase = mockk()

    private fun item(id: String = "abc") = MediaItem(
        id = id,
        sourceUrl = "https://www.instagram.com/reel/$id/",
        directUrl = "https://cdn/$id.mp4",
        thumbnailUrl = "https://cdn/$id.jpg",
        type = MediaType.VIDEO,
        width = 720,
        height = 1280,
    )

    private fun savedStateHandle(url: String = "https://www.instagram.com/reel/abc/"): SavedStateHandle {
        val encoded = URLEncoder.encode(url, Charsets.UTF_8.name())
        return SavedStateHandle(mapOf(PreviewViewModel.ARG_ENCODED_URL to encoded))
    }

    @Test
    fun `initial fetch resolves to Success and populates state`() = runTest(mainDispatcher.testDispatcher) {
        val items = listOf(item())
        coEvery { fetch(any()) } returns FetchState.Success(items)

        val vm = PreviewViewModel(fetch, download, savedStateHandle())
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.fetch is FetchState.Success)
        assertEquals(items, (state.fetch as FetchState.Success).items)
    }

    @Test
    fun `OnRetry triggers another fetch call`() = runTest(mainDispatcher.testDispatcher) {
        var calls = 0
        coEvery { fetch(any()) } answers {
            calls++
            FetchState.Success(listOf(item()))
        }
        val vm = PreviewViewModel(fetch, download, savedStateHandle())
        advanceUntilIdle()
        vm.onEvent(PreviewEvent.OnRetry)
        advanceUntilIdle()
        assertEquals(2, calls)
    }

    @Test
    fun `OnDownload updates downloads map and emits ShowSnackbarWithOpen on Completed`() =
        runTest(mainDispatcher.testDispatcher) {
            val target = item()
            coEvery { fetch(any()) } returns FetchState.Success(listOf(target))
            every { download(target) } returns flowOf(
                DownloadStatus.Queued(target.id),
                DownloadStatus.Running(target.id, progressPct = 50),
                DownloadStatus.Completed(target.id, uri = "content://foo/1"),
            )

            val vm = PreviewViewModel(fetch, download, savedStateHandle())
            advanceUntilIdle()

            vm.effects.test {
                vm.onEvent(PreviewEvent.OnDownload(target))
                advanceUntilIdle()
                val effect = awaitItem()
                assertTrue(effect is PreviewEffect.ShowSnackbarWithOpen)
                assertEquals("content://foo/1", (effect as PreviewEffect.ShowSnackbarWithOpen).uri)
                cancelAndIgnoreRemainingEvents()
            }

            // Last status for item must be Completed in the downloads map.
            val status = vm.state.value.downloads[target.id]
            assertNotNull(status)
            assertTrue(status is DownloadStatus.Completed)
        }

    @Test
    fun `OnDownload emits ShowSnackbar on Failed`() = runTest(mainDispatcher.testDispatcher) {
        val target = item()
        coEvery { fetch(any()) } returns FetchState.Success(listOf(target))
        every { download(target) } returns flow {
            emit(DownloadStatus.Queued(target.id))
            emit(DownloadStatus.Failed(target.id, ErrorReason.Network))
        }

        val vm = PreviewViewModel(fetch, download, savedStateHandle())
        advanceUntilIdle()

        vm.effects.test {
            vm.onEvent(PreviewEvent.OnDownload(target))
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is PreviewEffect.ShowSnackbar)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
