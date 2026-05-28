package com.reelgrab.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Single Room database for the app.
 *
 * Why single-DB? We have one durable concept (download history). Settings live in
 * DataStore-Preferences. Adding more entities later just expands this list — we
 * intentionally don't pre-split databases by feature, which would force cross-DB
 * joins we'd then have to do manually in Kotlin.
 */
@Database(
    entities = [DownloadEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(MediaTypeConverter::class)
abstract class ReelGrabDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
