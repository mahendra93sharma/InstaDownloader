package com.reelgrab.feature.preview

import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelgrab.core.ui.permission.rememberPermissionGuardedAction
import kotlinx.coroutines.launch

/**
 * Stateful entry point for the preview screen.
 *
 * Why a single guarded dispatcher (rather than per-event)? The rationale dialog must appear
 * once per session, not once per tile; routing every download-class event through one gated
 * lambda keeps the prompt-once behaviour while still letting the ViewModel see which specific
 * event fired (per-item vs. download-all).
 */
@Composable
fun PreviewScreen(
    onBack: () -> Unit,
    onOpenUri: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PreviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PreviewEffect.OpenUri -> onOpenUri(Uri.parse(effect.uri))
                is PreviewEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is PreviewEffect.ShowSnackbarWithOpen -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Download complete",
                        actionLabel = "Open",
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) onOpenUri(Uri.parse(effect.uri))
                }
            }
        }
    }

    // Holds the next download event waiting for permission resolution.
    var pendingDownload by remember { mutableStateOf<PreviewEvent?>(null) }

    val guardedDispatch = rememberPermissionGuardedAction(
        rationaleTitle = "Permission needed",
        rationaleMessage = "ReelGrab needs this permission to show download progress and save media to your gallery.",
        onDenied = {
            pendingDownload = null
            scope.launch { snackbarHostState.showSnackbar("Permission required to download") }
        },
        action = {
            pendingDownload?.let(viewModel::onEvent)
            pendingDownload = null
        },
    )

    PreviewContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEvent = { event ->
            when (event) {
                is PreviewEvent.OnDownload, PreviewEvent.OnDownloadAll -> {
                    pendingDownload = event
                    guardedDispatch()
                }
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}
