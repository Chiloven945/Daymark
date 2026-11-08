package top.chiloven.daymark.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DateCalculatorTest {

    private val today = LocalDate.of(2026, 11, 8)

    private val dateCalculator = DateCalculator(
        object : TimeProvider {
            override fun today(): LocalDate =
                today

            override fun zoneId(): ZoneId =
                ZoneId.of("UTC")
        },
    )

    @Test
    fun `non recurring event today is TODAY`() {
        val occurrence = dateCalculator.occurrence(
            Event(title = "Today", date = today),
        )

        assertThat(occurrence.phase).isEqualTo(EventPhase.TODAY)
        assertThat(occurrence.days).isEqualTo(0L)
    }

    @Test
    fun `non recurring event in the future has positive days`() {
        val occurrence = dateCalculator.occurrence(
            Event(title = "Future", date = today.plusDays(5)),
        )

        assertThat(occurrence.phase).isEqualTo(EventPhase.UPCOMING)
        assertThat(occurrence.days).isEqualTo(5L)
    }

    @Test
    fun `count up event in the past has negative days`() {
        val occurrence = dateCalculator.occurrence(
            Event(
                title = "Started",
                date = today.minusDays(3),
                direction = CountDirection.COUNT_UP,
            ),
        )

        assertThat(occurrence.phase).isEqualTo(EventPhase.PASSED)
        assertThat(occurrence.days).isEqualTo(-3L)
        assertThat(occurrence.isCountUp).isTrue()
    }

    @Test
    fun `auto event switches to count up after target date`() {
        val occurrence = dateCalculator.occurrence(
            Event(
                title = "Auto",
                date = today.minusDays(2),
                direction = CountDirection.AUTO,
            ),
        )

        assertThat(occurrence.isCountUp).isTrue()
        assertThat(occurrence.days).isEqualTo(-2L)
    }

    @Test
    fun `yearly recurrence finds next occurrence`() {
        val event = Event(
            title = "Anniversary",
            date = LocalDate.of(2020, 5, 20),
            recurrence = RecurrenceRule(RecurrenceFrequency.YEARLY),
        )

        val occurrence = dateCalculator.occurrence(event)

        assertThat(occurrence.targetDate).isEqualTo(LocalDate.of(2027, 5, 20))
        assertThat(occurrence.anniversaryYear).isEqualTo(7)
    }

    @Test
    fun `yearly leap day resolves to last valid day`() {
        val event = Event(
            title = "Leap",
            date = LocalDate.of(2024, 2, 29),
            recurrence = RecurrenceRule(RecurrenceFrequency.YEARLY),
        )

        val occurrence = dateCalculator.occurrence(event)

        assertThat(occurrence.targetDate).isEqualTo(LocalDate.of(2027, 2, 28))
    }

    @Test
    fun `monthly recurrence handles month end`() {
        val event = Event(
            title = "Monthly",
            date = LocalDate.of(2026, 1, 31),
            recurrence = RecurrenceRule(RecurrenceFrequency.MONTHLY),
        )

        val dates = dateCalculator.occurrencesInRange(
            event = event,
            start = LocalDate.of(2026, 1, 1),
            endInclusive = LocalDate.of(2026, 4, 30),
        )

        assertThat(dates).containsExactly(
            LocalDate.of(2026, 1, 31),
            LocalDate.of(2026, 2, 28),
            LocalDate.of(2026, 3, 31),
            LocalDate.of(2026, 4, 30),
        ).inOrder()
    }

    @Test
    fun `recurrence stops after end date`() {
        val event = Event(
            title = "Limited",
            date = LocalDate.of(2026, 11, 8),
            recurrence = RecurrenceRule(
                frequency = RecurrenceFrequency.YEARLY,
                endDate = LocalDate.of(2027, 12, 31),
            ),
        )

        assertThat(dateCalculator.nextOccurrenceOrNull(event, LocalDate.of(2028, 1, 1)))
                .isNull()
    }

    @Test
    fun `occurrences in range returns only dates in range`() {
        val event = Event(
            title = "Weekly",
            date = LocalDate.of(2026, 11, 1),
            recurrence = RecurrenceRule(RecurrenceFrequency.WEEKLY),
        )

        val dates = dateCalculator.occurrencesInRange(
            event = event,
            start = LocalDate.of(2026, 11, 8),
            endInclusive = LocalDate.of(2026, 11, 22),
        )

        assertThat(dates).containsExactly(
            LocalDate.of(2026, 11, 8),
            LocalDate.of(2026, 11, 15),
            LocalDate.of(2026, 11, 22),
        ).inOrder()
    }

}
