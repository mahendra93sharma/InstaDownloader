package com.reelgrab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelgrab.domain.usecase.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Resolves the app's start destination from a single source of truth.
 *
 * Why null as the initial value? The disclaimer flag lives in DataStore which loads
 * asynchronously; rendering "home" or "disclaimer" before we know the user's status would
 * either show a one-frame flash of the wrong screen, or skip the disclaimer entirely on
 * re-installs. Null means "still loading" → host renders a splash placeholder.
 */
@HiltViewModel
class AppRootViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
) : ViewModel() {

    val startDestination: StateFlow<String?> = observeSettings.disclaimerAccepted
        .map { accepted -> if (accepted) Destination.Home.route else ROUTE_DISCLAIMER }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null,
        )

    companion object {
        const val ROUTE_DISCLAIMER: String = "disclaimer"
    }
}
