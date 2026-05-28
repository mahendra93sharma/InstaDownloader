package com.reelgrab.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelgrab.domain.model.DownloadRecord
import com.reelgrab.domain.model.MediaType
import com.reelgrab.domain.repository.HistoryRepository
import com.reelgrab.domain.usecase.ObserveHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the History tab.
 *
 * Why combine the records flow with a query StateFlow? Filtering happens reactively without
 * recomputing from a synchronous `search()` call each keystroke — Room emits the canonical
 * list and the query stream simply re-filters it client-side. This keeps the DAO surface
 * minimal and makes deletion update the visible list automatically.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val observeHistory: ObserveHistoryUseCase,
    private val historyRepo: HistoryRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<HistoryState> = combine(observeHistory(), query) { records, q ->
        HistoryState(
            records = records.filter { matches(it, q) },
            query = q,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = HistoryState(),
    )

    private val _effects = Channel<HistoryEffect>(Channel.BUFFERED)
    val effects: Flow<HistoryEffect> = _effects.receiveAsFlow()

    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.OnSearchChange -> query.update { event.query }
            is HistoryEvent.OnDelete -> viewModelScope.launch { historyRepo.delete(event.id) }
            is HistoryEvent.OnOpen -> viewModelScope.launch {
                _effects.send(HistoryEffect.OpenUri(event.record.savedUri, event.record.mimeType()))
            }
            is HistoryEvent.OnShare -> viewModelScope.launch {
                _effects.send(HistoryEffect.ShareUri(event.record.savedUri, event.record.mimeType()))
            }
        }
    }

    private fun matches(record: DownloadRecord, q: String): Boolean {
        if (q.isBlank()) return true
        val needle = q.trim().lowercase()
        return record.sourceUrl.lowercase().contains(needle) ||
            record.id.lowercase().contains(needle)
    }
}

internal fun DownloadRecord.mimeType(): String = when (type) {
    MediaType.VIDEO -> "video/*"
    MediaType.IMAGE -> "image/*"
}
