package com.reelgrab.feature.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.reelgrab.core.ui.theme.ReelGrabTheme
import com.reelgrab.domain.model.DownloadStatus
import com.reelgrab.domain.model.MediaItem
import com.reelgrab.domain.model.MediaType

/**
 * Single tile in the preview grid.
 *
 * Why is the action button overlaid on the image? Spec §3 mandates a single-tap download
 * affordance per tile; lifting the button out into a row below would double the cell
 * height and break the masonry layout on small screens.
 */
@Composable
fun MediaTile(
    item: MediaItem,
    status: DownloadStatus?,
    onDownload: () -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
            )
            TileBadge(type = item.type, modifier = Modifier.align(Alignment.TopStart))
            val duration = item.durationMs
            if (item.type == MediaType.VIDEO && duration != null) {
                DurationLabel(
                    durationMs = duration,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
            DownloadAffordance(
                status = status,
                onDownload = onDownload,
                onOpen = onOpen,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            )
        }
    }
}

@Composable
private fun TileBadge(type: MediaType, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(8.dp).size(28.dp),
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.55f),
    ) {
        Icon(
            imageVector = if (type == MediaType.VIDEO) Icons.Outlined.PlayCircle else Icons.Outlined.Image,
            contentDescription = type.name,
            tint = Color.White,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Composable
private fun DurationLabel(durationMs: Long, modifier: Modifier = Modifier) {
    val seconds = durationMs / 1000
    val label = "%d:%02d".format(seconds / 60, seconds % 60)
    Box(
        modifier = modifier
            .padding(8.dp)
            .background(Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text = label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DownloadAffordance(
    status: DownloadStatus?,
    onDownload: () -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (status) {
        null, is DownloadStatus.Queued -> FilledIconButton(onClick = onDownload, modifier = modifier) {
            Icon(Icons.Outlined.Download, contentDescription = "Download")
        }
        is DownloadStatus.Running -> Box(modifier = modifier.size(40.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { status.progressPct / 100f },
                modifier = Modifier.size(36.dp),
            )
        }
        is DownloadStatus.Completed -> FilledIconButton(
            onClick = { onOpen(status.uri) },
            modifier = modifier,
        ) { Icon(Icons.Filled.Check, contentDescription = "Open") }
        is DownloadStatus.Failed -> FilledIconButton(onClick = onDownload, modifier = modifier) {
            Icon(Icons.Outlined.Download, contentDescription = "Retry")
        }
    }
}

@Preview(name = "Tile – idle", showBackground = true)
@Composable
private fun TileIdlePreview() {
    ReelGrabTheme {
        MediaTile(
            item = MediaItem("a", "https://x", "https://x", "https://picsum.photos/300", MediaType.VIDEO, durationMs = 42_000, width = 1, height = 1),
            status = null,
            onDownload = {},
            onOpen = {},
        )
    }
}

@Preview(name = "Tile – downloading 60%", showBackground = true)
@Composable
private fun TileDownloadingPreview() {
    ReelGrabTheme {
        MediaTile(
            item = MediaItem("a", "https://x", "https://x", "https://picsum.photos/300", MediaType.VIDEO, durationMs = 42_000, width = 1, height = 1),
            status = DownloadStatus.Running("a", 60),
            onDownload = {},
            onOpen = {},
        )
    }
}

@Preview(name = "Tile – completed dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TileCompletedPreview() {
    ReelGrabTheme(darkTheme = true) {
        MediaTile(
            item = MediaItem("a", "https://x", "https://x", "https://picsum.photos/300", MediaType.IMAGE, width = 1, height = 1),
            status = DownloadStatus.Completed("a", "content://media/0"),
            onDownload = {},
            onOpen = {},
        )
    }
}
