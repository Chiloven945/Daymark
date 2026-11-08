package top.chiloven.daymark.core.model

import java.time.LocalDate
import java.time.LocalTime

enum class CountDirection {

    COUNT_DOWN,
    COUNT_UP,
    AUTO,

}

enum class RecurrenceFrequency {

    NONE,
    WEEKLY,
    MONTHLY,
    YEARLY,

}

data class RecurrenceRule(
    val frequency: RecurrenceFrequency = RecurrenceFrequency.NONE,
    val interval: Int = 1,
    val endDate: LocalDate? = null,
) {

    val isRepeating: Boolean get() = frequency != RecurrenceFrequency.NONE

}

data class Reminder(
    val offsetDays: Int = 0,
    val time: LocalTime = LocalTime.of(9, 0),
    val enabled: Boolean = true,
)

data class Category(
    val id: Long,
    val name: String,
    val colorArgb: Long,
    val iconKey: String = "event",
    val sortOrder: Int = 0,
)

data class Event(
    val id: Long = 0L,
    val title: String,
    val date: LocalDate,
    val direction: CountDirection = CountDirection.COUNT_DOWN,
    val recurrence: RecurrenceRule = RecurrenceRule(),
    val categoryId: Long? = null,
    val note: String = "",
    val isPinned: Boolean = false,
    val reminder: Reminder? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class EventPhase {

    UPCOMING,
    TODAY,
    PASSED,

}

data class EventOccurrence(
    val event: Event,
    val targetDate: LocalDate,
    /**
     * Signed day difference from today to [targetDate]. Positive means the
     * target is in the future, zero means today, negative means it has passed.
     */
    val days: Long,
    val phase: EventPhase,
    val anniversaryYear: Int? = null,
) {

    val isCountUp: Boolean
        get() = event.direction == CountDirection.COUNT_UP ||
                (event.direction == CountDirection.AUTO && days < 0)

}

data class EventDraft(
    val id: Long = 0L,
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    val direction: CountDirection = CountDirection.COUNT_DOWN,
    val recurrence: RecurrenceRule = RecurrenceRule(),
    val categoryId: Long? = null,
    val note: String = "",
    val isPinned: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderOffsetDays: Int = 0,
    val reminderTime: LocalTime = LocalTime.of(9, 0),
) {

    fun toEvent(
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis(),
    ): Event = Event(
        id = id,
        title = title.trim(),
        date = date,
        direction = direction,
        recurrence = recurrence,
        categoryId = categoryId,
        note = note.trim(),
        isPinned = isPinned,
        reminder = if (reminderEnabled) {
            Reminder(
                offsetDays = reminderOffsetDays,
                time = reminderTime,
                enabled = true,
            )
        } else {
            null
        },
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

}

fun Event.toDraft(): EventDraft = EventDraft(
    id = id,
    title = title,
    date = date,
    direction = direction,
    recurrence = recurrence,
    categoryId = categoryId,
    note = note,
    isPinned = isPinned,
    reminderEnabled = reminder != null,
    reminderOffsetDays = reminder?.offsetDays ?: 0,
    reminderTime = reminder?.time ?: LocalTime.of(9, 0),
)
