package com.reelgrab.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.reelgrab.domain.model.Quality
import com.reelgrab.domain.model.ThemeMode
import com.reelgrab.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-Preferences implementation of [SettingsRepository].
 *
 * Why string-encode enums? Enum ordinals are unstable across renames / reorders;
 * persisting the canonical name and `valueOf`-ing on read keeps user data safe
 * across refactors. We swallow `IllegalArgumentException` from `valueOf` and fall
 * back to the default so a corrupt prefs file never crashes app start.
 */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    override val defaultQuality: Flow<Quality> = dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_QUALITY]?.let { runCatching { Quality.valueOf(it) }.getOrNull() }
            ?: Quality.ORIGINAL
    }

    override val disclaimerAccepted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.DISCLAIMER_ACCEPTED] ?: false
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setDefaultQuality(quality: Quality) {
        dataStore.edit { it[Keys.DEFAULT_QUALITY] = quality.name }
    }

    override suspend fun setDisclaimerAccepted(accepted: Boolean) {
        dataStore.edit { it[Keys.DISCLAIMER_ACCEPTED] = accepted }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
    }
}
