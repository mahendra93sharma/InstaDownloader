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
 * Permission to request for the download flow on the current SDK level.
 *
 * Why API-gated? Spec §9 — `WRITE_EXTERNAL_STORAGE` is only needed (and granted) on API ≤28;
 * API 33+ needs `POST_NOTIFICATIONS` for the foreground download notification; API 29-32
 * needs neither. The caller asks for "the download permission" and we pick the right one.
 */
fun downloadPermissionForSdk(): String? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Manifest.permission.POST_NOTIFICATIONS
    Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> Manifest.permission.WRITE_EXTERNAL_STORAGE
    else -> null
}

/**
 * Wraps an action lambda with a just-in-time permission flow.
 *
 * Flow: 1. User taps the action. 2. If permission already granted, run the action immediately.
 * 3. Otherwise show a rationale dialog FIRST (spec §9). 4. On Continue, fire the system prompt.
 * 5. When the system returns granted, run the action. On denial, run `onDenied` for fallback UX.
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
    val permission = downloadPermissionForSdk()
        ?: return action // No permission required on this SDK; just run.

    var pendingAction by remember { mutableStateOf(false) }
    var showRationale by remember { mutableStateOf(false) }

    val permissionState = rememberPermissionState(permission) { granted ->
        if (granted && pendingAction) {
            pendingAction = false
            action()
        } else if (!granted) {
            pendingAction = false
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
                    onDenied()
                }) { Text("Cancel") }
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
