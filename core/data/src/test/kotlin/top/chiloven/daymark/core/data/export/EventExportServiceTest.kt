package top.chiloven.daymark.core.data.export

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import top.chiloven.daymark.core.model.CountDirection
import top.chiloven.daymark.core.model.Event
import top.chiloven.daymark.core.model.RecurrenceFrequency
import top.chiloven.daymark.core.model.RecurrenceRule
import top.chiloven.daymark.core.model.Reminder
import java.time.LocalDate
import java.time.LocalTime

class EventExportServiceTest {

    private val service = EventExportService()

    @Test
    fun `export and import round trip preserves event fields`() {
        val event = Event(
            id = 7L,
            title = "Anniversary",
            date = LocalDate.of(2026, 5, 20),
            direction = CountDirection.AUTO,
            recurrence = RecurrenceRule(
                frequency = RecurrenceFrequency.YEARLY,
                interval = 1,
                endDate = LocalDate.of(2030, 5, 20),
            ),
            categoryId = 2L,
            note = "Dinner",
            isPinned = true,
            reminder = Reminder(
                offsetDays = 3,
                time = LocalTime.of(18, 30),
            ),
        )

        val content = service.export(listOf(event), "2026-11-08T00:00:00Z")
        val imported = service.import(content)

        assertThat(imported).hasSize(1)
        assertThat(imported.first().title)
                .isEqualTo("Anniversary")
        assertThat(imported.first().date)
                .isEqualTo(LocalDate.of(2026, 5, 20))
        assertThat(imported.first().direction)
                .isEqualTo(CountDirection.AUTO)
        assertThat(imported.first().recurrence.frequency)
                .isEqualTo(RecurrenceFrequency.YEARLY)
        assertThat(imported.first().recurrence.endDate)
                .isEqualTo(LocalDate.of(2030, 5, 20))
        assertThat(imported.first().reminder?.offsetDays)
                .isEqualTo(3)
        assertThat(imported.first().reminder?.time)
                .isEqualTo(LocalTime.of(18, 30))
    }

    @Test
    fun `import rejects unsupported versions`() {
        val content = /* language=JSON */ """
            {
              "version": 99,
              "exportedAt": "2026-11-08T00:00:00Z",
              "events": []
            }
        """.trimIndent()

        val result = runCatching { service.import(content) }

        assertThat(result.isFailure).isTrue()
    }

}
