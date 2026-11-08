package top.chiloven.daymark.core.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * All calendar arithmetic lives here. UI, widgets, and reminders must not
 * duplicate recurrence or day-count logic.
 */
class DateCalculator(
    private val timeProvider: TimeProvider,
) {

    fun today(): LocalDate = timeProvider.today()

    fun occurrence(event: Event, from: LocalDate = today()): EventOccurrence {
        val target = nextOccurrenceOrNull(event, from) ?: event.date
        return occurrenceOn(event, target, from)
    }

    fun nextOccurrenceOrNull(event: Event, from: LocalDate = today()): LocalDate? {
        if (!event.recurrence.isRepeating) {
            return event.date
        }

        val endDate = event.recurrence.endDate
        var index = 0
        while (index < MAX_OCCURRENCES) {
            val candidate = occurrenceAt(event, index)
            if (endDate != null && candidate.isAfter(endDate)) return null
            if (!candidate.isBefore(from)) return candidate
            index++
        }
        return null
    }

    fun occurrenceOn(
        event: Event,
        targetDate: LocalDate,
        from: LocalDate = today(),
    ): EventOccurrence {
        val days = ChronoUnit.DAYS.between(from, targetDate)
        val phase = when {
            targetDate.isAfter(from) -> EventPhase.UPCOMING
            targetDate.isEqual(from) -> EventPhase.TODAY
            else -> EventPhase.PASSED
        }
        return EventOccurrence(
            event = event,
            targetDate = targetDate,
            days = days,
            phase = phase,
            anniversaryYear = anniversaryYear(event, targetDate),
        )
    }

    fun occurrencesInRange(
        event: Event,
        start: LocalDate,
        endInclusive: LocalDate,
    ): List<LocalDate> {
        if (endInclusive.isBefore(start)) return emptyList()
        if (!event.recurrence.isRepeating) {
            return if (event.date in start..endInclusive) listOf(event.date) else emptyList()
        }

        val endDate = event.recurrence.endDate
        val result = mutableListOf<LocalDate>()
        var index = 0
        while (index < MAX_OCCURRENCES) {
            val candidate = occurrenceAt(event, index)
            if (endDate != null && candidate.isAfter(endDate)) break
            if (candidate.isAfter(endInclusive)) break
            if (!candidate.isBefore(start)) result += candidate
            index++
        }
        return result
    }

    private fun occurrenceAt(event: Event, index: Int): LocalDate {
        val interval = event.recurrence.interval.coerceAtLeast(1).toLong()
        val amount = index.toLong() * interval
        return when (event.recurrence.frequency) {
            RecurrenceFrequency.NONE -> event.date
            RecurrenceFrequency.WEEKLY -> event.date.plusWeeks(amount)
            RecurrenceFrequency.MONTHLY -> event.date.plusMonths(amount)
            RecurrenceFrequency.YEARLY -> event.date.plusYears(amount)
        }
    }

    private fun anniversaryYear(event: Event, occurrenceDate: LocalDate): Int? {
        if (event.recurrence.frequency != RecurrenceFrequency.YEARLY) return null
        val years = occurrenceDate.year - event.date.year
        return years.takeIf { it > 0 }
    }

    companion object {

        private const val MAX_OCCURRENCES = 10_000

    }

}
