package top.chiloven.daymark.core.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import top.chiloven.daymark.core.data.db.EventDao
import top.chiloven.daymark.core.data.db.EventEntity
import top.chiloven.daymark.core.model.CountDirection
import top.chiloven.daymark.core.model.Event
import top.chiloven.daymark.core.model.RecurrenceFrequency
import top.chiloven.daymark.core.model.RecurrenceRule
import top.chiloven.daymark.core.model.Reminder
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
) : EventRepository {

    override fun observeAll(): Flow<List<Event>> =
        eventDao.observeAll().map { entities -> entities.map(EventEntity::toDomain) }

    override fun observeById(id: Long): Flow<Event?> =
        eventDao.observeById(id).map { it?.toDomain() }

    override fun search(query: String): Flow<List<Event>> =
        eventDao.search(query).map { entities -> entities.map(EventEntity::toDomain) }

    override suspend fun upsert(event: Event): Long =
        eventDao.upsert(event.toEntity())

    override suspend fun upsertAll(events: List<Event>) {
        eventDao.upsertAll(events.map(Event::toEntity))
    }

    override suspend fun delete(id: Long) {
        eventDao.deleteById(id)
    }

    override suspend fun deleteAll() {
        eventDao.deleteAll()
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        eventDao.setPinned(
            id = id,
            pinned = pinned,
            updatedAt = System.currentTimeMillis()
        )
    }

}

private fun EventEntity.toDomain(): Event = Event(
    id = id,
    title = title,
    date = LocalDate.ofEpochDay(dateEpochDay),
    direction = enumValueOrDefault(
        direction,
        CountDirection.COUNT_DOWN
    ),
    recurrence = RecurrenceRule(
        frequency = enumValueOrDefault(
            recurrenceFrequency,
            RecurrenceFrequency.NONE
        ),
        interval = recurrenceInterval.coerceAtLeast(1),
        endDate = recurrenceEndEpochDay?.let(LocalDate::ofEpochDay),
    ),
    categoryId = categoryId,
    note = note,
    isPinned = isPinned,
    reminder = reminderOffsetDays?.let { offset ->
        Reminder(
            offsetDays = offset,
            time = reminderTimeMinutes.toLocalTime(),
            enabled = true,
        )
    },
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun Event.toEntity(): EventEntity = EventEntity(
    id = id,
    title = title,
    dateEpochDay = date.toEpochDay(),
    direction = direction.name,
    recurrenceFrequency = recurrence.frequency.name,
    recurrenceInterval = recurrence.interval.coerceAtLeast(1),
    recurrenceEndEpochDay = recurrence.endDate?.toEpochDay(),
    categoryId = categoryId,
    note = note,
    isPinned = isPinned,
    reminderOffsetDays = reminder?.offsetDays,
    reminderTimeMinutes = reminder?.time?.toMinutesOfDay(),
    createdAt = createdAt,
    updatedAt = System.currentTimeMillis(),
)

private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: fallback

private fun Int?.toLocalTime(): LocalTime {
    val minutes = this?.coerceIn(0, 24 * 60 - 1) ?: (9 * 60)
    return LocalTime.of(minutes / 60, minutes % 60)
}

private fun LocalTime.toMinutesOfDay(): Int = hour * 60 + minute
