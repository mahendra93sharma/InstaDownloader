package com.reelgrab.feature.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelgrab.core.ui.userMessage
import com.reelgrab.domain.model.DownloadStatus
import com.reelgrab.domain.model.FetchState
import com.reelgrab.domain.model.MediaItem
import com.reelgrab.domain.usecase.DownloadMediaUseCase
import com.reelgrab.domain.usecase.FetchMediaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

/**
 * Orchestrates extraction + per-item download streams for the preview screen.
 *
 * Why decode the URL here? The URL is encoded by Home so it survives nav-arg parsing — the
 * ViewModel is the lowest layer that still has the original string available, so decoding
 * happens once at the boundary instead of being scattered across the use cases.
 */
@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val fetchMedia: FetchMediaUseCase,
    private val downloadMedia: DownloadMediaUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val url: String = decode(savedStateHandle.get<String>(ARG_ENCODED_URL).orEmpty())

    private val _state = MutableStateFlow(PreviewState(url = url))
    val state: StateFlow<PreviewState> = _state.asStateFlow()

    private val _effects = Channel<PreviewEffect>(Channel.BUFFERED)
    val effects: Flow<PreviewEffect> = _effects.receiveAsFlow()

    init { resolve() }

    fun onEvent(event: PreviewEvent) {
        when (event) {
            PreviewEvent.OnRetry -> resolve()
            is PreviewEvent.OnDownload -> startDownload(event.item)
            PreviewEvent.OnDownloadAll -> {
                (_state.value.fetch as? FetchState.Success)?.items?.forEach(::startDownload)
            }
            is PreviewEvent.OnOpenSavedFile -> viewModelScope.launch {
                _effects.send(PreviewEffect.OpenUri(event.uri))
            }
        }
    }

    private fun resolve() {
        _state.update { it.copy(fetch = FetchState.Loading) }
        viewModelScope.launch {
            _state.update { it.copy(fetch = fetchMedia(url)) }
        }
    }

    private fun startDownload(item: MediaItem) {
        viewModelScope.launch {
            downloadMedia(item).collect { status ->
                _state.update { it.copy(downloads = it.downloads + (item.id to status)) }
                when (status) {
                    is DownloadStatus.Completed -> _effects.send(PreviewEffect.ShowSnackbarWithOpen(status.uri))
                    is DownloadStatus.Failed -> _effects.send(PreviewEffect.ShowSnackbar(status.reason.userMessage()))
                    else -> Unit
                }
            }
        }
    }

    companion object {
        const val ARG_ENCODED_URL: String = "encodedUrl"
        private fun decode(encoded: String): String = runCatching {
            URLDecoder.decode(encoded, Charsets.UTF_8.name())
        }.getOrDefault(encoded)
    }
}
