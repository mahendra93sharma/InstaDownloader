package com.reelgrab.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful entry point for the Settings screen.
 *
 * Why a navigation callback rather than a NavController dependency? Keeping nav out of the
 * feature module avoids a circular dependency on `:app` and means the Composable can be hosted
 * by anything (sub-nav graphs, dialogs, tests).
 */
@Composable
fun SettingsScreen(
    onShowDisclaimer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateToDisclaimer -> onShowDisclaimer()
            }
        }
    }

    SettingsContent(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}
