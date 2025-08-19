package studio.atopthehill.osom.services

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import studio.atopthehill.osom.OsomApplication
import studio.atopthehill.osom.data.db.entity.TaskStatus
import java.time.LocalDateTime

class NudgeActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_AS_DONE = "studio.atopthehill.osom.services.action.MARK_AS_DONE"
        const val EXTRA_TASK_ID = "studio.atopthehill.osom.services.extra.TASK_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        if (taskId == -1L) {
            return
        }

        val appRepository = (context.applicationContext as OsomApplication).appRepository
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val scope = CoroutineScope(Dispatchers.IO)

        when (intent.action) {
            ACTION_MARK_AS_DONE -> {
                scope.launch {
                    appRepository.updateTaskStatus(taskId, TaskStatus.COMPLETED)
                    notificationManager.cancel(taskId.toInt())
                }
            }
        }
    }
}
