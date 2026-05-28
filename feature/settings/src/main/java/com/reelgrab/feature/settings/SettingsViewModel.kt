package com.reelgrab.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelgrab.domain.usecase.ObserveSettingsUseCase
import com.reelgrab.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Mediates between the Settings UI and DataStore-backed [ObserveSettingsUseCase].
 *
 * Why split observe/update? Reads must be reactive (theme changes recolour the whole app
 * instantly); writes are imperative one-shots. Two use cases keeps each call site obvious.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
) : ViewModel() {

    val state: StateFlow<SettingsState> = combine(
        observeSettings.themeMode,
        observeSettings.defaultQuality,
        observeSettings.disclaimerAccepted,
    ) { theme, quality, accepted ->
        SettingsState(themeMode = theme, defaultQuality = quality, disclaimerAccepted = accepted)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = SettingsState(),
    )

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnThemeChange -> viewModelScope.launch {
                updateSettings.setThemeMode(event.mode)
            }
            is SettingsEvent.OnQualityChange -> viewModelScope.launch {
                updateSettings.setDefaultQuality(event.quality)
            }
            SettingsEvent.OnReviewDisclaimer -> viewModelScope.launch {
                _effects.send(SettingsEffect.NavigateToDisclaimer)
            }
        }
    }
}
