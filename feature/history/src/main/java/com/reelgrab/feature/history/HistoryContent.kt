package com.reelgrab.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import com.reelgrab.core.ui.components.EmptyState
import com.reelgrab.core.ui.theme.ReelGrabTheme
import com.reelgrab.domain.model.DownloadRecord
import com.reelgrab.domain.model.MediaType

/**
 * Stateless body of the History screen.
 *
 * Why extract? Previews + UI tests instantiate this directly with hand-rolled state; the
 * Composable carries no Hilt graph and no side effects.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    state: HistoryState,
    snackbarHostState: SnackbarHostState,
    onEvent: (HistoryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("History") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SearchField(query = state.query, onQueryChange = { onEvent(HistoryEvent.OnSearchChange(it)) })
            HistoryBody(state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        placeholder = { Text("Search by URL") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )
}

@Composable
private fun HistoryBody(state: HistoryState, onEvent: (HistoryEvent) -> Unit) {
    when {
        state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.records.isEmpty() -> EmptyState(
            title = "No downloads yet",
            subtitle = "Saved Instagram and Facebook media will appear here.",
            modifier = Modifier.fillMaxSize(),
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.records, key = { it.id }) { record ->
                HistoryRow(
                    record = record,
                    onOpen = { onEvent(HistoryEvent.OnOpen(record)) },
                    onShare = { onEvent(HistoryEvent.OnShare(record)) },
                    onDelete = { onEvent(HistoryEvent.OnDelete(record.id)) },
                )
            }
        }
    }
}

private val sampleRecords = listOf(
    DownloadRecord(
        id = "abc123",
        sourceUrl = "https://www.instagram.com/reel/Cxyz/",
        savedUri = "content://media/external/video/0/42",
        type = MediaType.VIDEO,
        savedAt = 1_716_000_000_000,
        sizeBytes = 12_300_000,
    ),
    DownloadRecord(
        id = "def456",
        sourceUrl = "https://www.facebook.com/watch/?v=abc",
        savedUri = "content://media/external/images/0/43",
        type = MediaType.IMAGE,
        savedAt = 1_715_900_000_000,
        sizeBytes = 870_000,
    ),
)

@Preview(name = "History – populated", showBackground = true)
@Composable
private fun HistoryContentPopulatedPreview() {
    ReelGrabTheme {
        Surface {
            HistoryContent(
                state = HistoryState(records = sampleRecords, isLoading = false),
                snackbarHostState = remember { SnackbarHostState() },
                onEvent = {},
            )
        }
    }
}

@Preview(name = "History – empty dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HistoryContentEmptyDarkPreview() {
    ReelGrabTheme(darkTheme = true) {
        Surface {
            HistoryContent(
                state = HistoryState(records = emptyList(), isLoading = false),
                snackbarHostState = remember { SnackbarHostState() },
                onEvent = {},
            )
        }
    }
}
