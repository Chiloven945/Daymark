package top.chiloven.daymark.core.data.reminder

import top.chiloven.daymark.core.model.Event

interface ReminderScheduler {

    suspend fun schedule(event: Event)

    suspend fun cancel(eventId: Long)

    suspend fun rescheduleAll()

}
