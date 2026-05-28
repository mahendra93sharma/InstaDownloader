package com.reelgrab.feature.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Primary call-to-action button.
 *
 * Why disabled rather than hidden? Material guidance keeps primary actions in a predictable
 * location even when invalid — disabling communicates "you're close" without layout shift.
 */
@Composable
internal fun FetchButton(state: HomeState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = state.urlValid && !state.isLoading,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(text = if (state.isLoading) "Fetching..." else "Fetch")
    }
}
