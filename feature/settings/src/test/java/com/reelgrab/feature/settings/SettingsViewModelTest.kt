package com.reelgrab.feature.settings

import app.cash.turbine.test
import com.reelgrab.domain.model.Quality
import com.reelgrab.domain.model.ThemeMode
import com.reelgrab.domain.usecase.ObserveSettingsUseCase
import com.reelgrab.domain.usecase.UpdateSettingsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherRule()

    private fun observer(
        theme: ThemeMode = ThemeMode.SYSTEM,
        quality: Quality = Quality.ORIGINAL,
        accepted: Boolean = false,
    ): ObserveSettingsUseCase = mockk {
        every { themeMode } returns flowOf(theme)
        every { defaultQuality } returns flowOf(quality)
        every { disclaimerAccepted } returns flowOf(accepted)
    }

    @Test
    fun `initial state combines all three flows`() = runTest(mainDispatcher.testDispatcher) {
        val observe = observer(theme = ThemeMode.DARK, quality = Quality.HIGH, accepted = true)
        val update: UpdateSettingsUseCase = mockk(relaxed = true)
        val vm = SettingsViewModel(observe, update)

        vm.state.test {
            awaitItem() // initial empty
            advanceUntilIdle()
            val populated = awaitItem()
            assertEquals(ThemeMode.DARK, populated.themeMode)
            assertEquals(Quality.HIGH, populated.defaultQuality)
            assertEquals(true, populated.disclaimerAccepted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnThemeChange calls update setThemeMode`() = runTest(mainDispatcher.testDispatcher) {
        val update: UpdateSettingsUseCase = mockk(relaxed = true)
        coEvery { update.setThemeMode(any()) } returns Unit

        val vm = SettingsViewModel(observer(), update)
        vm.onEvent(SettingsEvent.OnThemeChange(ThemeMode.LIGHT))
        advanceUntilIdle()

        coVerify(exactly = 1) { update.setThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun `OnQualityChange calls update setDefaultQuality`() = runTest(mainDispatcher.testDispatcher) {
        val update: UpdateSettingsUseCase = mockk(relaxed = true)
        coEvery { update.setDefaultQuality(any()) } returns Unit

        val vm = SettingsViewModel(observer(), update)
        vm.onEvent(SettingsEvent.OnQualityChange(Quality.MEDIUM))
        advanceUntilIdle()

        coVerify(exactly = 1) { update.setDefaultQuality(Quality.MEDIUM) }
    }

    @Test
    fun `OnReviewDisclaimer emits NavigateToDisclaimer`() = runTest(mainDispatcher.testDispatcher) {
        val update: UpdateSettingsUseCase = mockk(relaxed = true)
        val vm = SettingsViewModel(observer(), update)

        vm.effects.test {
            vm.onEvent(SettingsEvent.OnReviewDisclaimer)
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is SettingsEffect.NavigateToDisclaimer)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
