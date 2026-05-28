package com.reelgrab.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.reelgrab.core.ui.components.ErrorCard
import com.reelgrab.core.ui.theme.ReelGrabTheme
import com.reelgrab.core.ui.userMessage
import com.reelgrab.domain.model.ErrorReason

/**
 * Stateless body of the Home screen.
 *
 * Why factor this out? The `@Preview` matrix (empty, valid, clipboard hint, loading, error)
 * needs to instantiate the UI with arbitrary state and no Hilt graph — keeping this Composable
 * pure makes the previews trivial and means UI tests can drive it without a ViewModel.
 */
@Composable
fun HomeContent(
    state: HomeState,
    snackbarHostState: SnackbarHostState,
    onEvent: (HomeEvent) -> Unit,
    onPasteFromClipboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("ReelGrab", style = MaterialTheme.typography.headlineMedium)
                UrlField(
                    state = state,
                    onEvent = onEvent,
                    onPasteFromClipboard = onPasteFromClipboard,
                )
                FetchButton(state = state, onClick = { onEvent(HomeEvent.OnSubmit) })
                if (state.clipboardSuggestion != null && state.url.isBlank()) {
                    ClipboardHintCard(
                        url = state.clipboardSuggestion,
                        onAccept = { onEvent(HomeEvent.OnAcceptClipboardSuggestion) },
                        onDismiss = { onEvent(HomeEvent.OnDismissClipboardHint) },
                    )
                }
                if (state.fetchError != null) {
                    ErrorCard(
                        message = state.fetchError.userMessage(),
                        onRetry = { onEvent(HomeEvent.OnSubmit) },
                    )
                }
            }
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Preview(name = "Home – empty", showBackground = true)
@Composable
private fun HomeContentEmptyPreview() {
    ReelGrabTheme {
        Surface { HomeContent(state = HomeState(), snackbarHostState = remember { SnackbarHostState() }, onEvent = {}, onPasteFromClipboard = {}) }
    }
}

@Preview(name = "Home – clipboard hint", showBackground = true)
@Composable
private fun HomeContentClipboardPreview() {
    ReelGrabTheme {
        Surface {
            HomeContent(
                state = HomeState(clipboardSuggestion = "https://www.instagram.com/reel/ABC123/"),
                snackbarHostState = remember { SnackbarHostState() },
                onEvent = {},
                onPasteFromClipboard = {},
            )
        }
    }
}

@Preview(name = "Home – loading", showBackground = true)
@Composable
private fun HomeContentLoadingPreview() {
    ReelGrabTheme {
        Surface {
            HomeContent(
                state = HomeState(url = "https://www.instagram.com/reel/ABC123/", urlValid = true, isLoading = true),
                snackbarHostState = remember { SnackbarHostState() },
                onEvent = {},
                onPasteFromClipboard = {},
            )
        }
    }
}

@Preview(name = "Home – error", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeContentErrorDarkPreview() {
    ReelGrabTheme(darkTheme = true) {
        Surface {
            HomeContent(
                state = HomeState(
                    url = "https://www.instagram.com/reel/ABC123/",
                    urlValid = true,
                    fetchError = ErrorReason.Network,
                ),
                snackbarHostState = remember { SnackbarHostState() },
                onEvent = {},
                onPasteFromClipboard = {},
            )
        }
    }
}
