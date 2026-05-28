package com.reelgrab.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.reelgrab.core.ui.theme.ReelGrabTheme
import com.reelgrab.domain.model.Quality
import com.reelgrab.domain.model.ThemeMode

/**
 * Stateless body of the Settings screen.
 *
 * Why segmented buttons and not dropdowns? Both Theme and Quality have ≤3 mutually exclusive
 * choices — segmented controls reveal all options without a second tap and match Material 3
 * spec for "small enumerated set".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Settings") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppearanceSection(themeMode = state.themeMode, onChange = { onEvent(SettingsEvent.OnThemeChange(it)) })
            DownloadSection(quality = state.defaultQuality, onChange = { onEvent(SettingsEvent.OnQualityChange(it)) })
            LegalSection(onReview = { onEvent(SettingsEvent.OnReviewDisclaimer) })
            AboutSection()
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSection(themeMode: ThemeMode, onChange: (ThemeMode) -> Unit) {
    SectionHeader("Appearance")
    val options = ThemeMode.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = themeMode == mode,
                onClick = { onChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(mode.label())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadSection(quality: Quality, onChange: (Quality) -> Unit) {
    SectionHeader("Download")
    val options = Quality.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, q ->
            SegmentedButton(
                selected = quality == q,
                onClick = { onChange(q) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(q.label())
            }
        }
    }
    Text(
        text = "Applies when multiple renditions are available.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun LegalSection(onReview: () -> Unit) {
    SectionHeader("Legal")
    Surface(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onReview),
            headlineContent = { Text("Review disclaimer") },
            supportingContent = { Text("Terms governing your use of ReelGrab") },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
            },
        )
    }
}

@Composable
private fun AboutSection() {
    SectionHeader("About")
    Surface(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text("Version") },
            supportingContent = { Text(AppVersion) },
        )
    }
}

internal fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

internal fun Quality.label(): String = when (this) {
    Quality.ORIGINAL -> "Original"
    Quality.HIGH -> "High"
    Quality.MEDIUM -> "Medium"
}

// The :feature:settings module doesn't depend on :app BuildConfig, so we keep a placeholder
// constant. The app surface can override by injecting its own About row later.
private const val AppVersion: String = "0.1.0"

@Preview(name = "Settings – default", showBackground = true)
@Composable
private fun SettingsContentPreview() {
    ReelGrabTheme {
        Surface { SettingsContent(state = SettingsState(), onEvent = {}) }
    }
}

@Preview(name = "Settings – dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsContentDarkPreview() {
    ReelGrabTheme(darkTheme = true) {
        Surface {
            SettingsContent(
                state = SettingsState(themeMode = ThemeMode.DARK, defaultQuality = Quality.HIGH),
                onEvent = {},
            )
        }
    }
}
