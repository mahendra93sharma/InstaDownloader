package com.reelgrab.feature.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.reelgrab.core.ui.components.ErrorCard
import com.reelgrab.core.ui.components.LoadingShimmer
import com.reelgrab.core.ui.theme.ReelGrabTheme
import com.reelgrab.core.ui.userMessage
import com.reelgrab.domain.model.ErrorReason
import com.reelgrab.domain.model.FetchState
import com.reelgrab.domain.model.MediaItem
import com.reelgrab.domain.model.MediaType

/**
 * Stateless body of the Preview screen.
 *
 * Why a fixed adaptive grid? `GridCells.Adaptive` handles phones/tablets without needing a
 * WindowSizeClass dependency — fewer moving parts in v1, easy to swap in later if we need
 * smarter breakpoints.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewContent(
    state: PreviewState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEvent: (PreviewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.fetch is FetchState.Success) {
                ExtendedFloatingActionButton(
                    onClick = { onEvent(PreviewEvent.OnDownloadAll) },
                    icon = { Icon(Icons.Outlined.DownloadForOffline, contentDescription = null) },
                    text = { Text("Download all") },
                )
            }
        },
    ) { padding ->
        PreviewBody(
            fetch = state.fetch,
            downloads = state.downloads,
            padding = padding,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun PreviewBody(
    fetch: FetchState,
    downloads: Map<String, com.reelgrab.domain.model.DownloadStatus>,
    padding: PaddingValues,
    onEvent: (PreviewEvent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        when (fetch) {
            FetchState.Idle, FetchState.Loading -> ShimmerGrid()
            is FetchState.Error -> ErrorCard(
                message = fetch.reason.userMessage(),
                onRetry = { onEvent(PreviewEvent.OnRetry) },
                modifier = Modifier.padding(16.dp),
            )
            is FetchState.Success -> SuccessGrid(items = fetch.items, downloads = downloads, onEvent = onEvent)
        }
    }
}

@Composable
private fun ShimmerGrid() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(6) {
            LoadingShimmer(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                cornerRadiusDp = 16,
            )
        }
    }
}

@Composable
private fun SuccessGrid(
    items: List<MediaItem>,
    downloads: Map<String, com.reelgrab.domain.model.DownloadStatus>,
    onEvent: (PreviewEvent) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.id }) { item ->
            MediaTile(
                item = item,
                status = downloads[item.id],
                onDownload = { onEvent(PreviewEvent.OnDownload(item)) },
                onOpen = { uri -> onEvent(PreviewEvent.OnOpenSavedFile(uri)) },
            )
        }
    }
}

private val sampleItems = listOf(
    MediaItem("1", "https://x", "https://x", "https://picsum.photos/300", MediaType.VIDEO, durationMs = 32_000, width = 1080, height = 1920),
    MediaItem("2", "https://x", "https://x", "https://picsum.photos/301", MediaType.IMAGE, width = 1080, height = 1080),
    MediaItem("3", "https://x", "https://x", "https://picsum.photos/302", MediaType.VIDEO, durationMs = 12_500, width = 720, height = 1280),
)

@Preview(name = "Preview – success", showBackground = true)
@Composable
private fun PreviewContentSuccessPreview() {
    ReelGrabTheme {
        Surface {
            PreviewContent(
                state = PreviewState(url = "https://www.instagram.com/reel/X", fetch = FetchState.Success(sampleItems)),
                snackbarHostState = remember { SnackbarHostState() },
                onBack = {},
                onEvent = {},
            )
        }
    }
}

@Preview(name = "Preview – loading dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewContentLoadingPreview() {
    ReelGrabTheme(darkTheme = true) {
        Surface {
            PreviewContent(
                state = PreviewState(url = "https://x", fetch = FetchState.Loading),
                snackbarHostState = remember { SnackbarHostState() },
                onBack = {},
                onEvent = {},
            )
        }
    }
}

@Preview(name = "Preview – error", showBackground = true)
@Composable
private fun PreviewContentErrorPreview() {
    ReelGrabTheme {
        Surface {
            PreviewContent(
                state = PreviewState(url = "https://x", fetch = FetchState.Error(ErrorReason.Network)),
                snackbarHostState = remember { SnackbarHostState() },
                onBack = {},
                onEvent = {},
            )
        }
    }
}
