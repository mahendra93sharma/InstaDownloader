package com.reelgrab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.reelgrab.core.ui.theme.ReelGrabTheme
import com.reelgrab.ui.ReelGrabApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host.
 *
 * Why a single activity? Compose Navigation handles all screen transitions; multiple
 * activities would force us to duplicate Hilt graph + theming + edge-to-edge config. We
 * intentionally call `enableEdgeToEdge()` before `setContent` so Compose receives correct
 * window insets from frame zero.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ReelGrabTheme {
                ReelGrabApp()
            }
        }
    }
}
