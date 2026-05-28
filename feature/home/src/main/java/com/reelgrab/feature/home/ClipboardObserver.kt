package com.reelgrab.feature.home

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.reelgrab.domain.usecase.ValidateUrlUseCase

/**
 * Lifecycle-aware clipboard sniffer.
 *
 * Why ON_RESUME and not a permanent listener? On Android 10+ accessing the clipboard
 * outside foreground triggers a privacy toast — even reading once per `onResume` is the
 * minimum needed to honour the "auto-detect pasted link" UX while staying inside the
 * platform's idle-clipboard rules. We discard anything that doesn't match the canonical
 * URL regexes so a random text snippet is never surfaced to the user as a suggestion.
 */
@Composable
fun ClipboardObserver(onCandidate: (String?) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onCandidate(readClipboardCandidate(context))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

private fun readClipboardCandidate(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    val text = clip.getItemAt(0).coerceToText(context)?.toString()?.trim().orEmpty()
    if (text.isEmpty()) return null
    return when {
        ValidateUrlUseCase.INSTAGRAM.matches(text) -> text
        ValidateUrlUseCase.FACEBOOK.matches(text) -> text
        else -> null
    }
}
