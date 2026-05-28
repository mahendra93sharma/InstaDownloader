package com.reelgrab.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelgrab.domain.model.FetchState
import com.reelgrab.domain.usecase.FetchMediaUseCase
import com.reelgrab.domain.usecase.ValidateUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLEncoder
import javax.inject.Inject

/**
 * Orchestrator for the URL-input screen.
 *
 * Why split validation (sync) from fetch (async)? Live regex validation must happen on every
 * keystroke without paying for a network round-trip, while the actual resolve only fires on
 * submit. Modelling them as two distinct use cases keeps the ViewModel decision tree small
 * and makes test-doubles trivial.
 *
 * Effects are exposed via a Channel — not a StateFlow — so that navigation/snackbars fire
 * exactly once even across config change replay.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val validate: ValidateUrlUseCase,
    private val fetchMedia: FetchMediaUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnUrlChange -> applyUrl(event.value)
            is HomeEvent.OnPaste -> applyUrl(event.value, clearSuggestion = true)
            HomeEvent.OnAcceptClipboardSuggestion -> {
                val suggestion = _state.value.clipboardSuggestion ?: return
                applyUrl(suggestion, clearSuggestion = true)
            }
            HomeEvent.OnClear -> _state.update {
                it.copy(url = "", urlValid = false, source = null, fetchError = null)
            }
            HomeEvent.OnDismissClipboardHint -> _state.update { it.copy(clipboardSuggestion = null) }
            HomeEvent.OnSubmit -> submit()
        }
    }

    /** Wired from the screen after the clipboard observer reads a candidate URL. */
    fun setClipboardSuggestion(value: String?) {
        // Don't overwrite if the user has already started typing.
        if (_state.value.url.isNotBlank() && value != null) return
        _state.update { it.copy(clipboardSuggestion = value) }
    }

    private fun applyUrl(raw: String, clearSuggestion: Boolean = false) {
        val result = validate(raw)
        _state.update {
            it.copy(
                url = raw,
                urlValid = result.isSuccess,
                source = result.getOrNull(),
                fetchError = null,
                clipboardSuggestion = if (clearSuggestion) null else it.clipboardSuggestion,
            )
        }
    }

    private fun submit() {
        val current = _state.value
        if (!current.urlValid) {
            viewModelScope.launch { _effects.send(HomeEffect.ShowSnackbar("Enter a valid Instagram or Facebook link")) }
            return
        }
        _state.update { it.copy(isLoading = true, fetchError = null) }
        viewModelScope.launch {
            when (val result = fetchMedia(current.url)) {
                is FetchState.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    val encoded = URLEncoder.encode(current.url, Charsets.UTF_8.name())
                    _effects.send(HomeEffect.NavigateToPreview(encoded))
                }
                is FetchState.Error -> _state.update {
                    it.copy(isLoading = false, fetchError = result.reason)
                }
                FetchState.Idle, FetchState.Loading -> _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
