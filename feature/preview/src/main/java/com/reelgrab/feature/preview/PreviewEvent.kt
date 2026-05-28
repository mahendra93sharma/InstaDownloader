package com.reelgrab.feature.preview

import com.reelgrab.domain.model.MediaItem

/**
 * Closed set of user intents accepted by [PreviewViewModel].
 *
 * Why sealed? The screen has only four buttons, and the exhaustive `when` in the ViewModel
 * guarantees that adding a new affordance (e.g. share) forces the developer to handle it.
 */
sealed interface PreviewEvent {
    data object OnRetry : PreviewEvent
    data class OnDownload(val item: MediaItem) : PreviewEvent
    data object OnDownloadAll : PreviewEvent
    data class OnOpenSavedFile(val uri: String) : PreviewEvent
}
