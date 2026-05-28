package com.reelgrab.data.local

import com.reelgrab.domain.model.DownloadRecord
import com.reelgrab.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [HistoryRepository] that maps between [DownloadEntity] and the
 * transport-neutral domain [DownloadRecord].
 *
 * Why entity <-> domain mapping here? Tests can drop a fake DAO and verify the
 * mapping in isolation, and we never leak Room annotations into the domain.
 */
@Singleton
class RoomHistoryRepository @Inject constructor(
    private val dao: DownloadDao,
) : HistoryRepository {

    override fun stream(): Flow<List<DownloadRecord>> =
        dao.stream().map { list -> list.map { it.toDomain() } }

    override suspend fun add(record: DownloadRecord) {
        dao.upsert(record.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }

    override suspend fun search(query: String): List<DownloadRecord> =
        dao.search(query).map { it.toDomain() }

    private fun DownloadEntity.toDomain() = DownloadRecord(
        id = id,
        sourceUrl = sourceUrl,
        savedUri = savedUri,
        type = type,
        savedAt = savedAt,
        sizeBytes = sizeBytes,
    )

    private fun DownloadRecord.toEntity() = DownloadEntity(
        id = id,
        sourceUrl = sourceUrl,
        savedUri = savedUri,
        type = type,
        savedAt = savedAt,
        sizeBytes = sizeBytes,
    )
}
