package com.reelgrab.feature.history

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful entry point.
 *
 * Why route effects through callbacks instead of dispatching intents inline? The Composable
 * stays Android-framework-free for tests; the host (app module) owns the platform glue that
 * actually fires `ACTION_VIEW` / `ACTION_SEND` intents.
 */
@Composable
fun HistoryScreen(
    onOpenUri: (uri: String, mimeType: String) -> Unit,
    onShareUri: (uri: String, mimeType: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HistoryEffect.OpenUri -> onOpenUri(effect.uri, effect.mimeType)
                is HistoryEffect.ShareUri -> onShareUri(effect.uri, effect.mimeType)
                is HistoryEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    HistoryContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}
