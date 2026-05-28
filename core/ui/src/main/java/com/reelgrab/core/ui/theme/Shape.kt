package com.reelgrab.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Brand shape scale.
 *
 * Why these radii? 16dp on cards matches the marketing spec; 28dp on `large` aligns FAB and
 * extended FAB corners to the Material 3 expressive recommendation. Keeping `small` at 8dp
 * preserves crisp affordances on chips/snackbars where larger radii feel out of scale.
 */
val ReelGrabShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)
