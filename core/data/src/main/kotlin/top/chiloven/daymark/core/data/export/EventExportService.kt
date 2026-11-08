package top.chiloven.daymark.core.data.export

import top.chiloven.daymark.core.model.CountDirection
import top.chiloven.daymark.core.model.Event
import top.chiloven.daymark.core.model.RecurrenceFrequency
import top.chiloven.daymark.core.model.RecurrenceRule
import top.chiloven.daymark.core.model.Reminder
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ExportPayload(
    val version: Int = 1,
    val exportedAt: String,
    val events: List<ExportEvent>,
)

@Serializable
data class ExportEvent(
    val id: Long,
    val title: String,
    val date: String,
    val direction: String,
    val recurrenceFrequency: String,
    val recurrenceInterval: Int,
    val recurrenceEndDate: String? = null,
    val categoryId: Long? = null,
    val note: String = "",
    val isPinned: Boolean = false,
    val reminderOffsetDays: Int? = null,
    val reminderTime: String? = null,
)

class EventExportService @Inject constructor() {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun export(events: List<Event>, now: String): String {
        val payload = ExportPayload(
            exportedAt = now,
            events = events.map { it.toExportEvent() },
        )
        return json.encodeToString(payload)
    }

    fun import(content: String): List<Event> {
        val payload = json.decodeFromString<ExportPayload>(content)
        require(payload.version <= CURRENT_VERSION) {
            "Unsupported export version: ${payload.version}"
        }
        return payload.events.map { it.toDomain() }
    }

    private fun Event.toExportEvent(): ExportEvent = ExportEvent(
        id = id,
        title = title,
        date = date.toString(),
        direction = direction.name,
        recurrenceFrequency = recurrence.frequency.name,
        recurrenceInterval = recurrence.interval,
        recurrenceEndDate = recurrence.endDate?.toString(),
        categoryId = categoryId,
        note = note,
        isPinned = isPinned,
        reminderOffsetDays = reminder?.offsetDays,
        reminderTime = reminder?.time?.toString(),
    )

    private fun ExportEvent.toDomain(): Event = Event(
        id = id,
        title = title,
        date = LocalDate.parse(date),
        direction = CountDirection.entries.firstOrNull { it.name == direction }
            ?: CountDirection.COUNT_DOWN,
        recurrence = RecurrenceRule(
            frequency = RecurrenceFrequency.entries
                    .firstOrNull { it.name == recurrenceFrequency }
                ?: RecurrenceFrequency.NONE,
            interval = recurrenceInterval.coerceAtLeast(1),
            endDate = recurrenceEndDate?.let(LocalDate::parse),
        ),
        categoryId = categoryId,
        note = note,
        isPinned = isPinned,
        reminder = reminderOffsetDays?.let { offset ->
            Reminder(
                offsetDays = offset,
                time = reminderTime?.let(LocalTime::parse) ?: LocalTime.of(9, 0),
                enabled = true,
            )
        },
    )

    private companion object {

        const val CURRENT_VERSION = 1
    }

}
