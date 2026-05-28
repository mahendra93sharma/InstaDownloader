package com.reelgrab.domain.model

/**
 * Default download quality preference selected in Settings.
 *
 * Why: when the extractor returns multiple renditions, this preference tells the data layer
 * which one to enqueue without prompting the user every time. `ORIGINAL` preserves the
 * upstream master file; `HIGH` / `MEDIUM` map to lower-bitrate variants when available.
 */
enum class Quality {
    ORIGINAL,
    HIGH,
    MEDIUM,
}
