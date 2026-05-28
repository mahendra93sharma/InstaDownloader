package com.reelgrab.feature.preview

/**
 * One-shot effects emitted by [PreviewViewModel].
 *
 * Why separate `OpenUri` and `ShowSnackbarWithOpen`? Tapping a tile that's already finished
 * should jump straight to the gallery; tapping "Open" on a freshly completed snackbar takes
 * a different code path. Keeping the channels distinct avoids hidden coupling.
 */
sealed interface PreviewEffect {
    data class OpenUri(val uri: String) : PreviewEffect
    data class ShowSnackbarWithOpen(val uri: String) : PreviewEffect
    data class ShowSnackbar(val message: String) : PreviewEffect
}
