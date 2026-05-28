package com.reelgrab.feature.home

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelgrab.domain.usecase.ValidateUrlUseCase

/**
 * Stateful entry point.
 *
 * Why split this from [HomeContent]? Hilt + lifecycle wiring live here, the body stays pure.
 * Side-effects (navigation, snackbars) are pulled off the ViewModel channel via [LaunchedEffect]
 * so they fire exactly once and never replay on config change.
 */
@Composable
fun HomeScreen(
    onNavigateToPreview: (encodedUrl: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    ClipboardObserver(onCandidate = { viewModel.setClipboardSuggestion(it) })

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToPreview -> onNavigateToPreview(effect.encodedUrl)
                is HomeEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    HomeContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onPasteFromClipboard = {
            val text = readClipboardText(context)
            if (text != null) viewModel.onEvent(HomeEvent.OnPaste(text))
        },
        modifier = modifier,
    )
}

private fun readClipboardText(context: Context): String? {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = cm.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    val text = clip.getItemAt(0).coerceToText(context)?.toString()?.trim().orEmpty()
    if (text.isEmpty()) return null
    return when {
        ValidateUrlUseCase.INSTAGRAM.matches(text) -> text
        ValidateUrlUseCase.FACEBOOK.matches(text) -> text
        else -> text // user explicitly tapped paste — accept anything and let validator decide.
    }
}
