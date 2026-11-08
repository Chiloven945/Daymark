package top.chiloven.daymark.core.data.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import top.chiloven.daymark.core.data.repository.EventRepository
import top.chiloven.daymark.core.data.repository.SettingsRepository
import top.chiloven.daymark.core.model.DateCalculator
import top.chiloven.daymark.core.model.Event
import top.chiloven.daymark.core.model.TimeProvider
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class AlarmReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventRepository: EventRepository,
    private val settingsRepository: SettingsRepository,
    private val dateCalculator: DateCalculator,
    private val timeProvider: TimeProvider,
) : ReminderScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    override suspend fun schedule(event: Event) {
        val reminder = event.reminder
        if (reminder == null || !settingsRepository.settings.first().notificationsEnabled) {
            cancel(event.id)
            return
        }

        val triggerAt = nextReminderDateTime(event)
        if (triggerAt == null) {
            cancel(event.id)
            return
        }

        val pendingIntent = reminderPendingIntent(event)

        val canExact = alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt.atZone(timeProvider.zoneId()).toInstant().toEpochMilli(),
                pendingIntent,
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt.atZone(timeProvider.zoneId()).toInstant().toEpochMilli(),
                pendingIntent,
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.CUPCAKE)
    override suspend fun cancel(eventId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    override suspend fun rescheduleAll() {
        eventRepository.observeAll().first().forEach { event ->
            cancel(event.id)
            schedule(event)
        }
    }

    private fun reminderPendingIntent(event: Event): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            putExtra(ReminderReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, event.title)
            putExtra(ReminderReceiver.EXTRA_DATE, event.date.toString())
        }
        return PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nextReminderDateTime(event: Event): LocalDateTime? {
        val reminder = event.reminder
            ?: return null
        val now = LocalDateTime.now(timeProvider.zoneId())
        var occurrence = dateCalculator.nextOccurrenceOrNull(event, timeProvider.today())
            ?: return null
        var reminderDate = occurrence.minusDays(reminder.offsetDays.toLong())
        var reminderDateTime = LocalDateTime.of(reminderDate, reminder.time)

        if (!reminderDateTime.isAfter(now) && event.recurrence.isRepeating) {
            occurrence = dateCalculator.nextOccurrenceOrNull(event, occurrence.plusDays(1))
                ?: return null
            reminderDate = occurrence.minusDays(reminder.offsetDays.toLong())
            reminderDateTime = LocalDateTime.of(reminderDate, reminder.time)
        }
        return reminderDateTime.takeIf { it.isAfter(now) }
    }

}
