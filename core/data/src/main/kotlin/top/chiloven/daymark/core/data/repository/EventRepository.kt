package top.chiloven.daymark.core.data.repository

import top.chiloven.daymark.core.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {

    fun observeAll(): Flow<List<Event>>

    fun observeById(id: Long): Flow<Event?>

    fun search(query: String): Flow<List<Event>>

    suspend fun upsert(event: Event): Long

    suspend fun upsertAll(events: List<Event>)

    suspend fun delete(id: Long)

    suspend fun deleteAll()

    suspend fun setPinned(id: Long, pinned: Boolean)

}
