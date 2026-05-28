package com.reelgrab.domain.usecase

import com.reelgrab.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Re-exposes the [SettingsRepository] to features without leaking the repo interface.
 *
 * Why: keeping use cases as the only public surface means features can't reach past them into
 * the repo (which would let them call setters by accident). The Settings screen pairs this
 * observer with [UpdateSettingsUseCase] for mutations.
 */
class ObserveSettingsUseCase @Inject constructor(
    private val settings: SettingsRepository,
) {
    val themeMode = settings.themeMode
    val defaultQuality = settings.defaultQuality
    val disclaimerAccepted = settings.disclaimerAccepted
}
