package com.reelgrab.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for [DownloadEntity].
 *
 * Why expose Flows? The History screen subscribes via `collectAsStateWithLifecycle`,
 * and Room emits a new list on every transaction touching the table — no manual
 * invalidation needed.
 */
@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY savedAt DESC")
    fun stream(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE sourceUrl LIKE '%' || :query || '%' ORDER BY savedAt DESC")
    suspend fun search(query: String): List<DownloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: String)
}
