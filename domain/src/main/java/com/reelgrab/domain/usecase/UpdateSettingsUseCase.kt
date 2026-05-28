package com.reelgrab.domain.usecase

import com.reelgrab.domain.model.Quality
import com.reelgrab.domain.model.ThemeMode
import com.reelgrab.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Mutation-side counterpart to [ObserveSettingsUseCase].
 *
 * Why: splitting read and write into two use cases keeps each one obvious from its name and
 * limits the surface a feature can accidentally depend on (the Theme provider only needs the
 * observer; only the Settings screen needs this writer).
 */
class UpdateSettingsUseCase @Inject constructor(
    private val settings: SettingsRepository,
) {
    suspend fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)
    suspend fun setDefaultQuality(quality: Quality) = settings.setDefaultQuality(quality)
    suspend fun setDisclaimerAccepted(accepted: Boolean) = settings.setDisclaimerAccepted(accepted)
}
