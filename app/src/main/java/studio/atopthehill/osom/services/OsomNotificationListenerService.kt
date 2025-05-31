package studio.atopthehill.osom.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import studio.atopthehill.osom.config.LogConfig
import studio.atopthehill.osom.utils.FileLogger

class OsomNotificationListenerService : NotificationListenerService() {

    private val TAG = "OsomNotificationListener"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Notification Listener Service created.")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "onListenerConnected: Notification Listener Service connected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (LogConfig.logNotifications) {
            val packageName = sbn.packageName ?: "N/A"
            val notification = sbn.notification
            val extras = notification?.extras

            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "N/A"
            val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "N/A"
            val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: "N/A"
            val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: "N/A"
            val ticker = notification?.tickerText?.toString() ?: "N/A"

            val logMessage =
                    "Notification Posted: Pkg[$packageName], Title[$title], Text[$text], SubText[$subText], BigText[$bigText], Ticker[$ticker], ID[${sbn.id}], Key[${sbn.key}]"
            Log.d(TAG, logMessage)
            FileLogger.log(TAG, logMessage)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (LogConfig.logNotifications) {
            val packageName = sbn.packageName ?: "N/A"
            val logMessage =
                    "Notification Removed: Pkg[$packageName], ID[${sbn.id}], Key[${sbn.key}]"
            Log.d(TAG, logMessage)
            FileLogger.log(TAG, logMessage)
        }
    }

    override fun onNotificationRemoved(
            sbn: StatusBarNotification?,
            rankingMap: RankingMap?,
            reason: Int
    ) {
        if (sbn == null) return
        if (LogConfig.logNotifications) {
            val packageName = sbn.packageName ?: "N/A"
            val reasonString = notificationRemovalReasonToString(reason)
            val logMessage =
                    "Notification Removed (with reason): Pkg[$packageName], ID[${sbn.id}], Key[${sbn.key}], Reason[$reasonString]"
            Log.d(TAG, logMessage)
            FileLogger.log(TAG, logMessage)
        }
    }

    private fun notificationRemovalReasonToString(reason: Int): String {
        return when (reason) {
            REASON_CLICK -> "REASON_CLICK"
            REASON_CANCEL -> "REASON_CANCEL"
            REASON_CANCEL_ALL -> "REASON_CANCEL_ALL"
            REASON_ERROR -> "REASON_ERROR"
            REASON_PACKAGE_CHANGED -> "REASON_PACKAGE_CHANGED"
            REASON_USER_STOPPED -> "REASON_USER_STOPPED"
            REASON_PACKAGE_BANNED -> "REASON_PACKAGE_BANNED"
            REASON_APP_CANCEL -> "REASON_APP_CANCEL"
            REASON_APP_CANCEL_ALL -> "REASON_APP_CANCEL_ALL"
            REASON_LISTENER_CANCEL -> "REASON_LISTENER_CANCEL"
            REASON_LISTENER_CANCEL_ALL -> "REASON_LISTENER_CANCEL_ALL"
            REASON_GROUP_SUMMARY_CANCELED -> "REASON_GROUP_SUMMARY_CANCELED"
            REASON_GROUP_OPTIMIZATION -> "REASON_GROUP_OPTIMIZATION"
            REASON_PACKAGE_SUSPENDED -> "REASON_PACKAGE_SUSPENDED"
            REASON_PROFILE_TURNED_OFF -> "REASON_PROFILE_TURNED_OFF"
            REASON_UNAUTOBUNDLED -> "REASON_UNAUTOBUNDLED"
            REASON_CHANNEL_BANNED -> "REASON_CHANNEL_BANNED"
            REASON_SNOOZED -> "REASON_SNOOZED"
            REASON_TIMEOUT -> "REASON_TIMEOUT"
            REASON_CHANNEL_REMOVED -> "REASON_CHANNEL_REMOVED"
            else -> "UNKNOWN_REASON ($reason)"
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "onListenerDisconnected: Notification Listener Service disconnected.")
        FileLogger.log(TAG, "onListenerDisconnected: Notification Listener Service disconnected.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Notification Listener Service destroyed.")
        FileLogger.log(TAG, "onDestroy: Notification Listener Service destroyed.")
    }
}
