package com.reelgrab.domain.repository

import com.reelgrab.domain.model.Quality
import com.reelgrab.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Boundary over DataStore-Preferences for user-facing app settings.
 *
 * Why: settings are observed reactively (theme + default quality drive UI in real time) and
 * mutated transactionally by the Settings screen. Flows + suspend setters keep the contract
 * symmetric with the Android DataStore API while shielding the domain from DataStore types.
 */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val defaultQuality: Flow<Quality>

    /**
     * Whether the user has accepted the first-launch disclaimer (spec §15.7). The app gates
     * access to extraction behind this flag until it flips true.
     */
    val disclaimerAccepted: Flow<Boolean>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDefaultQuality(quality: Quality)
    suspend fun setDisclaimerAccepted(accepted: Boolean)
}
