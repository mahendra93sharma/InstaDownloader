package com.reelgrab.domain.usecase

import com.reelgrab.domain.model.DownloadRecord
import com.reelgrab.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Thin wrapper that exposes the history stream to the UI layer.
 *
 * Why: even pass-through use cases are kept as classes so the dependency arrow always points
 * from feature -> domain -> repository (never feature -> repository). This keeps Hilt graphs
 * uniform and makes future cross-cutting changes (caching, analytics) trivial to add.
 */
class ObserveHistoryUseCase @Inject constructor(
    private val history: HistoryRepository,
) {
    operator fun invoke(): Flow<List<DownloadRecord>> = history.stream()
}
