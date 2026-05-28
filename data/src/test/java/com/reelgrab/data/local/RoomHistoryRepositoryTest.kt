package com.reelgrab.data.local

import com.reelgrab.domain.model.DownloadRecord
import com.reelgrab.domain.model.MediaType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoomHistoryRepositoryTest {

    /**
     * Fake DAO used in lieu of a Room in-memory DB (which would require Robolectric).
     * Tests the mapping logic in [RoomHistoryRepository] without booting Android.
     */
    private class FakeDownloadDao : DownloadDao {
        val entities = MutableStateFlow<List<DownloadEntity>>(emptyList())
        var lastDeleted: String? = null
            private set

        override fun stream(): Flow<List<DownloadEntity>> = entities

        override suspend fun search(query: String): List<DownloadEntity> =
            entities.value.filter { it.sourceUrl.contains(query, ignoreCase = true) }

        override suspend fun upsert(entity: DownloadEntity) {
            entities.value = entities.value.filterNot { it.id == entity.id } + entity
        }

        override suspend fun delete(id: String) {
            lastDeleted = id
            entities.value = entities.value.filterNot { it.id == id }
        }
    }

    private fun record(id: String) = DownloadRecord(
        id = id,
        sourceUrl = "https://www.instagram.com/reel/$id/",
        savedUri = "content://media/$id",
        type = MediaType.VIDEO,
        savedAt = 1_700_000_000_000L,
        sizeBytes = 1024L,
    )

    @Test
    fun `add then stream returns the persisted record mapped to domain`() = runTest {
        val dao = FakeDownloadDao()
        val repo = RoomHistoryRepository(dao)

        repo.add(record("a"))
        val records = repo.stream().first()

        assertEquals(1, records.size)
        assertEquals("a", records.first().id)
        assertEquals(MediaType.VIDEO, records.first().type)
    }

    @Test
    fun `search filters by source url substring`() = runTest {
        val dao = FakeDownloadDao()
        val repo = RoomHistoryRepository(dao)
        repo.add(record("aaa"))
        repo.add(record("bbb"))

        val results = repo.search("aaa")

        assertEquals(1, results.size)
        assertEquals("aaa", results.first().id)
    }

    @Test
    fun `delete forwards id to dao and removes record from stream`() = runTest {
        val dao = FakeDownloadDao()
        val repo = RoomHistoryRepository(dao)
        repo.add(record("a"))
        repo.add(record("b"))

        repo.delete("a")

        assertEquals("a", dao.lastDeleted)
        val remaining = repo.stream().first()
        assertEquals(1, remaining.size)
        assertTrue(remaining.none { it.id == "a" })
    }
}
