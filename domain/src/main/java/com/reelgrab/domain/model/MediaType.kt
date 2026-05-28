package com.reelgrab.domain.model

/**
 * Classifies a [MediaItem] for downstream UI affordances and storage routing.
 *
 * Why: the app saves videos to `Movies/ReelGrab/` and images to `Pictures/ReelGrab/`, and tile
 * decorations (badge, duration overlay, play icon) differ by type. Keeping the kind as a typed
 * enum avoids stringly-typed comparisons everywhere in the UI and download layers.
 */
enum class MediaType {
    VIDEO,
    IMAGE,
}
