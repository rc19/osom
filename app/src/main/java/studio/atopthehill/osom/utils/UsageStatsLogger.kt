package studio.atopthehill.osom.utils

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat // For consistent date formatting
import java.util.Date // For Date object with timestamp
import java.util.Locale // For Locale in SimpleDateFormat
import studio.atopthehill.osom.config.LogConfig

object UsageStatsLogger {

    private const val TAG = "OSOMUsageStatsLogger" // Changed tag for clarity

    // durationMillis: How far back to query for events.
    fun logRecentUsageStats(
            context: Context,
            durationMillis: Long = 1000 * 60 * 5
    ) { // Default to last 5 minutes
        if (!LogConfig.logUsageStats) { // Master switch from config
            // Log.d(TAG, "Usage stats logging is globally disabled in LogConfig.")
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            Log.w(TAG, "UsageStatsManager requires API level 22 (Lollipop MR1) or higher.")
            return
        }

        val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usageStatsManager == null) {
            Log.e(TAG, "UsageStatsManager not available.")
            return
        }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - durationMillis
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

        Log.d(
                TAG,
                "Querying all usage events from ${timeFormat.format(Date(startTime))} to ${timeFormat.format(Date(endTime))}"
        )

        try {
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            if (usageEvents == null) {
                Log.e(TAG, "queryEvents returned null. Check PACKAGE_USAGE_STATS permission.")
                return
            }

            val event = UsageEvents.Event()
            var eventCount = 0

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                eventCount++
                val eventTypeString = getEventTypeString(event.eventType)
                Log.d(
                        TAG,
                        "Usage Event: Pkg[${event.packageName ?: "N/A"}], Class[${event.className ?: "N/A"}], Type[$eventTypeString], Time[${timeFormat.format(Date(event.timeStamp))}]"
                )
            }

            if (eventCount == 0) {
                Log.d(
                        TAG,
                        "No general usage events found in the last ${durationMillis / (1000*60)} min."
                )
            } else {
                Log.d(TAG, "Logged $eventCount general usage events.")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Ensure PACKAGE_USAGE_STATS permission is granted.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception while querying usage stats.", e)
        }
    }

    private fun getEventTypeString(eventType: Int): String {
        return when (eventType) {
            UsageEvents.Event.NONE -> "NONE"
            UsageEvents.Event.ACTIVITY_RESUMED -> "ACTIVITY_RESUMED (FG)"
            UsageEvents.Event.ACTIVITY_PAUSED -> "ACTIVITY_PAUSED (BG)"
            UsageEvents.Event.ACTIVITY_STOPPED -> "ACTIVITY_STOPPED"
            UsageEvents.Event.CONFIGURATION_CHANGE -> "CONFIGURATION_CHANGE"
            UsageEvents.Event.DEVICE_SHUTDOWN -> "DEVICE_SHUTDOWN"
            UsageEvents.Event.DEVICE_STARTUP -> "DEVICE_STARTUP"
            UsageEvents.Event.FOREGROUND_SERVICE_START -> "FG_SERVICE_START"
            UsageEvents.Event.FOREGROUND_SERVICE_STOP -> "FG_SERVICE_STOP"
            UsageEvents.Event.KEYGUARD_HIDDEN -> "KEYGUARD_HIDDEN (Unlocked)"
            UsageEvents.Event.KEYGUARD_SHOWN -> "KEYGUARD_SHOWN (Locked)"
            UsageEvents.Event.SCREEN_INTERACTIVE -> "SCREEN_INTERACTIVE (Screen ON)"
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> "SCREEN_NON_INTERACTIVE (Screen OFF)"
            UsageEvents.Event.SHORTCUT_INVOCATION -> "SHORTCUT_INVOCATION"
            UsageEvents.Event.STANDBY_BUCKET_CHANGED -> "STANDBY_BUCKET_CHANGED"
            UsageEvents.Event.USER_INTERACTION -> "USER_INTERACTION"
            else -> "UNKNOWN_EVENT ($eventType)"
        }
    }

    // You might want to add other utility functions here, for example, to get foreground app, or
    // aggregate stats.
}
