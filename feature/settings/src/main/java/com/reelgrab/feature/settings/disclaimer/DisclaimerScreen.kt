package com.reelgrab.feature.settings.disclaimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reelgrab.core.ui.theme.ReelGrabTheme

/**
 * First-launch disclaimer + Settings re-entry surface.
 *
 * Why two callbacks (`onAccept`, `onDecline`)? The host decides what each means — at first
 * launch decline finishes the activity, while from Settings decline is just pop-back. The
 * Composable doesn't need to know which case it's in.
 */
@Composable
fun DisclaimerScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DisclaimerViewModel = hiltViewModel(),
) {
    DisclaimerContent(
        onAccept = { viewModel.onAccept(onAccept) },
        onDecline = onDecline,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisclaimerContent(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Before you begin") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DisclaimerBody()
            ActionRow(onAccept = onAccept, onDecline = onDecline)
        }
    }
}

@Composable
private fun DisclaimerBody() {
    Text("Use responsibly", style = MaterialTheme.typography.headlineSmall)
    Text(
        text = "ReelGrab is provided for downloading PUBLIC media that you own or are otherwise authorised to save.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Text(
        text = "By continuing you agree to:",
        style = MaterialTheme.typography.titleMedium,
    )
    val bullets = listOf(
        "Respect Instagram's and Facebook's Terms of Service.",
        "Not bypass authentication — no login flows or private content.",
        "Not redistribute copyrighted media without the rights holder's consent.",
    )
    bullets.forEach { line ->
        Text(text = "•  $line", style = MaterialTheme.typography.bodyMedium)
    }
    Text(
        text = "You can revisit this notice anytime from Settings.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ActionRow(onAccept: () -> Unit, onDecline: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) { Text("Accept and continue") }
        OutlinedButton(onClick = onDecline, modifier = Modifier.fillMaxWidth()) { Text("Decline") }
    }
}

@Preview(name = "Disclaimer", showBackground = true)
@Composable
private fun DisclaimerPreview() {
    ReelGrabTheme {
        Surface { DisclaimerContent(onAccept = {}, onDecline = {}) }
    }
}

@Preview(name = "Disclaimer – dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DisclaimerDarkPreview() {
    ReelGrabTheme(darkTheme = true) {
        Surface { DisclaimerContent(onAccept = {}, onDecline = {}) }
    }
}
