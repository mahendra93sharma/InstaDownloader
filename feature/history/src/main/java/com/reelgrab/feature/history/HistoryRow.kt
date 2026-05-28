package com.reelgrab.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.reelgrab.core.ui.theme.ReelGrabTheme
import com.reelgrab.domain.model.DownloadRecord
import com.reelgrab.domain.model.MediaType
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Single history row.
 *
 * Why include the overflow menu inline (vs. swipe-to-reveal)? Material 3 lists prefer
 * explicit affordances on platforms where gesture discovery is poor; the three actions
 * (Open / Share / Delete) fit cleanly in a popup without occupying primary row real estate.
 */
@Composable
fun HistoryRow(
    record: DownloadRecord,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = record.savedUri,
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
            )
            RowText(record = record, modifier = Modifier.weight(1f))
            OverflowMenu(onOpen = onOpen, onShare = onShare, onDelete = onDelete)
        }
    }
}

@Composable
private fun RowText(record: DownloadRecord, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        androidx.compose.foundation.layout.Column {
            Text(
                text = record.titleText(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = record.subtitleText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OverflowMenu(onOpen: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "More")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Open") }, onClick = { expanded = false; onOpen() })
            DropdownMenuItem(text = { Text("Share") }, onClick = { expanded = false; onShare() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { expanded = false; onDelete() })
        }
    }
}

internal fun DownloadRecord.titleText(): String {
    // Display the last path segment if present, otherwise the id.
    val tail = sourceUrl.trimEnd('/').substringAfterLast('/', missingDelimiterValue = id)
    return if (tail.isBlank()) id else tail
}

internal fun DownloadRecord.subtitleText(): String {
    val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
        .format(Date(savedAt))
    return "$date  ·  ${humanSize(sizeBytes)}"
}

private fun humanSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return "%.1f %s".format(value, units[unit])
}

@Preview(name = "Row", showBackground = true)
@Composable
private fun HistoryRowPreview() {
    ReelGrabTheme {
        Surface {
            HistoryRow(
                record = DownloadRecord(
                    id = "abc123",
                    sourceUrl = "https://www.instagram.com/reel/Cxyz/",
                    savedUri = "content://media/external/video/0/42",
                    type = MediaType.VIDEO,
                    savedAt = 1_716_000_000_000,
                    sizeBytes = 12_300_000,
                ),
                onOpen = {}, onShare = {}, onDelete = {},
            )
        }
    }
}
