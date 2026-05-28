package com.reelgrab.feature.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Pure-presentation URL field with paste affordance + live validation glyph.
 *
 * Why a separate Composable? The Home body stays under the 80-line cap; the field can be
 * reused later (e.g. share-target sheet) without bringing the rest of the screen along.
 */
@Composable
internal fun UrlField(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    onPasteFromClipboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showInvalid = state.url.isNotBlank() && !state.urlValid
    OutlinedTextField(
        value = state.url,
        onValueChange = { onEvent(HomeEvent.OnUrlChange(it)) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = showInvalid,
        placeholder = { Text("Paste Instagram or Facebook link") },
        leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
        trailingIcon = {
            ValidityTrailingIcon(
                state = state,
                onPaste = onPasteFromClipboard,
                onClear = { onEvent(HomeEvent.OnClear) },
            )
        },
        supportingText = {
            if (showInvalid) Text("Not a recognised Instagram or Facebook URL.")
            else if (state.urlValid) Text("${state.source?.name?.lowercase()?.replaceFirstChar { it.uppercase() }} link looks good.")
            else Text(" ")
        },
    )
}

@Composable
private fun ValidityTrailingIcon(
    state: HomeState,
    onPaste: () -> Unit,
    onClear: () -> Unit,
) {
    when {
        state.url.isBlank() -> IconButton(onClick = onPaste) {
            Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste from clipboard")
        }
        state.urlValid -> Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = "Valid URL",
            tint = MaterialTheme.colorScheme.primary,
        )
        else -> IconButton(onClick = onClear) {
            Icon(
                imageVector = Icons.Outlined.Cancel,
                contentDescription = "Clear",
                tint = errorTint(),
            )
        }
    }
}

@Composable
private fun errorTint(): Color = MaterialTheme.colorScheme.error
