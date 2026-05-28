package com.reelgrab.core.ui.permission

import android.Manifest
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Permission descriptor for the download flow on the current SDK level.
 *
 * `blocking = true`  → download cannot proceed without grant (legacy storage write).
 * `blocking = false` → download proceeds either way; permission only enables a side-effect
 *                      such as a progress notification.
 */
data class DownloadPermission(val name: String, val blocking: Boolean)

/**
 * Permission to request for the download flow on the current SDK level.
 *
 * Why API-gated? Spec §9 — `WRITE_EXTERNAL_STORAGE` is only needed (and granted) on API ≤28
 * to write into shared storage; API 29+ uses scoped MediaStore which needs no permission for
 * the app's own inserts. API 33+ optionally needs `POST_NOTIFICATIONS` for the foreground
 * download notification — denial does not stop the download; the notification just stays
 * silent. The caller asks for "the download permission" and we pick the right one.
 */
fun downloadPermissionForSdk(): DownloadPermission? = when {
    // API ≤28: legacy storage write — must be granted to save into shared storage.
    Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ->
        DownloadPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, blocking = true)
    // API 29+: MediaStore inserts need no permission. POST_NOTIFICATIONS is purely a
    // UX nicety (progress chip) and would be asked separately if/when the user opts
    // into progress notifications. Don't gate the download on it.
    else -> null
}

/**
 * Wraps an action lambda with a just-in-time permission flow.
 *
 * Flow: 1. User taps the action. 2. If permission already granted, run the action immediately.
 * 3. Otherwise show a rationale dialog FIRST (spec §9). 4. On Continue, fire the system prompt.
 * 5. When the system returns granted, run the action. On denial of a non-blocking permission
 *    the action still runs (e.g. download works without notification permission). Only a
 *    blocking permission denial triggers `onDenied`.
 *
 * @return a `() -> Unit` to attach to the affordance.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberPermissionGuardedAction(
    rationaleTitle: String,
    rationaleMessage: String,
    onDenied: () -> Unit = {},
    action: () -> Unit,
): () -> Unit {
    val descriptor = downloadPermissionForSdk()
        ?: return action // No permission required on this SDK; just run.

    var pendingAction by remember { mutableStateOf(false) }
    var showRationale by remember { mutableStateOf(false) }

    val permissionState = rememberPermissionState(descriptor.name) { granted ->
        if (!pendingAction) return@rememberPermissionState
        pendingAction = false
        if (granted || !descriptor.blocking) {
            action()
        } else {
            onDenied()
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(rationaleTitle) },
            text = { Text(rationaleMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    pendingAction = true
                    permissionState.launchPermissionRequest()
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRationale = false
                    if (descriptor.blocking) onDenied() else action()
                }) { Text(if (descriptor.blocking) "Cancel" else "Skip") }
            },
        )
    }

    return {
        when (permissionState.status) {
            PermissionStatus.Granted -> action()
            else -> showRationale = true
        }
    }
}
