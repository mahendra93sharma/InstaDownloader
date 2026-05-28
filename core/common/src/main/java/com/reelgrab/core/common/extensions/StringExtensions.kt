package com.reelgrab.core.common.extensions

/**
 * Replaces every character outside `[A-Za-z0-9._-]` with `_`.
 *
 * Why: file names eventually land in MediaStore on user file systems (FAT32 USB,
 * SD cards, NTFS via OTG) which reject characters like `/ \ : * ? " < > |`.
 * Instagram and Facebook permalinks frequently embed slashes and query strings,
 * so naively using `MediaItem.id` as a file name corrupts the save. The regex
 * is intentionally conservative (ASCII-only) so the output is portable across
 * every reasonable downstream filesystem and never trips locale-sensitive
 * collation in `MediaScannerConnection`.
 */
fun String.sanitizeFileName(): String = replace(SANITIZER, "_")

private val SANITIZER = Regex("[^A-Za-z0-9._-]")
