package com.reelgrab.core.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.reelgrab.core.ui.theme.ReelGrabTheme

/**
 * Cheap shimmer placeholder for loading rectangles.
 *
 * Why no third-party lib? `accompanist-placeholder` is deprecated and pulling another
 * Brush animation library for ~30 LOC is wasteful. Pure `infiniteTransition` keeps the
 * dependency surface zero and recomposition cost minimal because only the Brush gradient
 * shifts each frame.
 */
@Composable
fun LoadingShimmer(
    modifier: Modifier = Modifier,
    cornerRadiusDp: Int = 16,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(animation = tween(1200)),
        label = "shimmer-translate",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translate - 400f, 0f),
        end = Offset(translate, 0f),
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadiusDp.dp))
            .background(brush),
    )
}

@Preview
@Composable
private fun LoadingShimmerPreview() {
    ReelGrabTheme {
        LoadingShimmer(modifier = Modifier.fillMaxWidth().height(80.dp))
    }
}
