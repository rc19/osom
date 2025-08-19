package studio.atopthehill.osom.utils.managers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import studio.atopthehill.osom.MainActivity
import studio.atopthehill.osom.R
import studio.atopthehill.osom.services.NudgeActionReceiver

class NudgeManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "osom_nudges"
        private const val CHANNEL_NAME = "Osom Nudges"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for Osom nudges"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showSilentNotification(taskText: String) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingContentIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a real icon later
            .setContentTitle("New Task Saved")
            .setContentText(taskText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .setContentIntent(pendingContentIntent)
            .setAutoCancel(true) // Automatically dismiss the notification when clicked
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showActiveNotification(taskId: Long, taskText: String) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingContentIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markAsDoneIntent = Intent(context, NudgeActionReceiver::class.java).apply {
            action = NudgeActionReceiver.ACTION_MARK_AS_DONE
            putExtra(NudgeActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val markAsDonePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            markAsDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a real icon later
            .setContentTitle("Task Reminder")
            .setContentText(taskText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingContentIntent)
            .setAutoCancel(true) // Automatically dismiss the notification when clicked
            .addAction(R.drawable.ic_launcher_foreground, "Mark as Done", markAsDonePendingIntent)
            .build()

        notificationManager.notify(taskId.toInt(), notification)
    }
}
