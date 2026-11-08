package top.chiloven.daymark.core.data.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import top.chiloven.daymark.core.data.R

class ReminderReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return

        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, 0L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val date = intent.getStringExtra(EXTRA_DATE).orEmpty()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )

        val contentIntent = PendingIntent.getActivity(
            context,
            eventId.toInt(),
            Intent(
                context,
                Class.forName("top.chiloven.daymark.MainActivity")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_EVENT_ID, eventId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(date)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

        notificationManager.notify(eventId.toInt(), notification)
    }

    companion object {

        const val ACTION_REMIND = "top.chiloven.daymark.action.REMIND"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_TITLE = "event_title"
        const val EXTRA_DATE = "event_date"
        private const val CHANNEL_ID = "daymark_reminders"

    }

}
