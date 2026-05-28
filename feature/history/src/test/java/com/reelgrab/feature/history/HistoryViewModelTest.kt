package com.reelgrab.feature.history

import app.cash.turbine.test
import com.reelgrab.domain.model.DownloadRecord
import com.reelgrab.domain.model.MediaType
import com.reelgrab.domain.repository.HistoryRepository
import com.reelgrab.domain.usecase.ObserveHistoryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherRule()

    private val historyRepo: HistoryRepository = mockk(relaxed = true)

    private fun record(id: String, sourceUrl: String) = DownloadRecord(
        id = id,
        sourceUrl = sourceUrl,
        savedUri = "content://media/$id",
        type = MediaType.VIDEO,
        savedAt = 1_700_000_000_000L,
        sizeBytes = 1024L,
    )

    @Test
    fun `initial collection emits records from observeHistory`() = runTest(mainDispatcher.testDispatcher) {
        val records = listOf(
            record("a", "https://www.instagram.com/reel/a/"),
            record("b", "https://www.facebook.com/watch/?v=b"),
        )
        val observe = mockk<ObserveHistoryUseCase>()
        every { observe.invoke() } returns flowOf(records)

        val vm = HistoryViewModel(observe, historyRepo)
        vm.state.test {
            // First emission is initial state.
            awaitItem()
            advanceUntilIdle()
            val populated = awaitItem()
            assertEquals(records, populated.records)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnSearchChange filters records by substring case-insensitive`() = runTest(mainDispatcher.testDispatcher) {
        val records = listOf(
            record("a", "https://www.instagram.com/reel/aaa/"),
            record("b", "https://www.facebook.com/watch/?v=bbb"),
        )
        val observe = mockk<ObserveHistoryUseCase>()
        every { observe.invoke() } returns MutableStateFlow(records)

        val vm = HistoryViewModel(observe, historyRepo)
        vm.state.test {
            awaitItem() // initial
            advanceUntilIdle()
            // Drain populated emission.
            skipItems(1)
            vm.onEvent(HistoryEvent.OnSearchChange("INSTAGRAM"))
            advanceUntilIdle()
            val filtered = awaitItem()
            assertEquals(1, filtered.records.size)
            assertEquals("a", filtered.records.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnDelete forwards id to history repository`() = runTest(mainDispatcher.testDispatcher) {
        val observe = mockk<ObserveHistoryUseCase>()
        every { observe.invoke() } returns flowOf(emptyList())
        coEvery { historyRepo.delete(any()) } returns Unit

        val vm = HistoryViewModel(observe, historyRepo)
        vm.onEvent(HistoryEvent.OnDelete("xyz"))
        advanceUntilIdle()

        coVerify(exactly = 1) { historyRepo.delete("xyz") }
    }

    @Test
    fun `OnOpen emits OpenUri effect with mime type`() = runTest(mainDispatcher.testDispatcher) {
        val observe = mockk<ObserveHistoryUseCase>()
        every { observe.invoke() } returns flowOf(emptyList())
        val target = record("a", "https://www.instagram.com/reel/a/")

        val vm = HistoryViewModel(observe, historyRepo)
        vm.effects.test {
            vm.onEvent(HistoryEvent.OnOpen(target))
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is HistoryEffect.OpenUri)
            assertEquals(target.savedUri, (effect as HistoryEffect.OpenUri).uri)
            assertEquals("video/*", effect.mimeType)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnShare emits ShareUri effect with mime type`() = runTest(mainDispatcher.testDispatcher) {
        val observe = mockk<ObserveHistoryUseCase>()
        every { observe.invoke() } returns flowOf(emptyList())
        val target = record("a", "https://www.instagram.com/reel/a/")

        val vm = HistoryViewModel(observe, historyRepo)
        vm.effects.test {
            vm.onEvent(HistoryEvent.OnShare(target))
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is HistoryEffect.ShareUri)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
