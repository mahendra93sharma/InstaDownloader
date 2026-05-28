package com.reelgrab.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.reelgrab.domain.model.MediaType

/**
 * Room representation of a completed download (history record).
 *
 * Why a separate entity (not the domain [com.reelgrab.domain.model.DownloadRecord])?
 * The Room compiler scans annotations on the entity; mixing those with the domain
 * type would force `@Entity` into `:domain` and contaminate it with Android deps.
 * Keeping them separate also lets us evolve the schema (rename, add columns)
 * without touching the domain class.
 */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val sourceUrl: String,
    val savedUri: String,
    val type: MediaType,
    val savedAt: Long,
    val sizeBytes: Long,
)

/** Persist [MediaType] as the enum name for forward-compatible schema evolution. */
class MediaTypeConverter {
    @TypeConverter
    fun fromMediaType(type: MediaType): String = type.name

    @TypeConverter
    fun toMediaType(raw: String): MediaType = MediaType.valueOf(raw)
}
