package studio.atopthehill.osom.services

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers // Ensure Dispatchers is imported
import kotlinx.coroutines.withContext // Ensure withContext is imported
import studio.atopthehill.osom.MainActivity // Import MainActivity for PendingIntent
import studio.atopthehill.osom.OsomApplication
import studio.atopthehill.osom.R // Import R class for icon
import studio.atopthehill.osom.data.repository.AppRepository

class AppTimerService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var appRepository: AppRepository
    private var currentTimer: CountDownTimer? = null
    private var currentTimingPackageName: String? = null
    private var currentRequestedDurationMinutes: Int? = null
    private var timerStartTime: LocalDateTime? = null
    private var currentAppUsageId: Long? = null

    companion object {
        const val ACTION_START_TIMER = "studio.atopthehill.osom.services.action.START_TIMER"
        const val ACTION_CANCEL_TIMER = "studio.atopthehill.osom.services.action.CANCEL_TIMER"
        const val ACTION_USER_RETURNED = "studio.atopthehill.osom.services.action.USER_RETURNED"
        const val EXTRA_PACKAGE_NAME = "studio.atopthehill.osom.services.extra.PACKAGE_NAME"
        const val EXTRA_REQUESTED_DURATION_MINUTES =
                "studio.atopthehill.osom.services.extra.REQUESTED_DURATION_MINUTES"
        const val EXTRA_APP_USAGE_ID = "studio.atopthehill.osom.services.extra.APP_USAGE_ID"

        private const val TAG = "AppTimerService"
        private const val MIN_DURATION_BEFORE_USER_CANCEL_SECS = 5L

        private const val TIMER_NOTIFICATION_CHANNEL_ID = "AppTimerServiceChannel"
        private const val TIMER_NOTIFICATION_ID = 1868 // For active timer
        private const val TIMER_FINISHED_NOTIFICATION_ID = 1869 // For when timer is done
        private const val GENERIC_SERVICE_NOTIFICATION_ID =
                1870 // For general service running state
    }

    override fun onCreate() {
        super.onCreate()
        appRepository = (application as OsomApplication).appRepository
        createNotificationChannel()
        Log.d(TAG, "AppTimerService Created")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(
                            TIMER_NOTIFICATION_CHANNEL_ID,
                            "OSOM App Timer",
                            NotificationManager.IMPORTANCE_HIGH // Use HIGH for timer finished
                    )
            channel.description = "Notifications for OSOM app usage timers."
            val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand received: ${intent?.action}")
        val action = intent?.action

        if (action == null && currentTimer == null) {
            // Service restarted by system, no active timer. Show generic notification and attempt
            // to stop.
            Log.d(
                    TAG,
                    "Service restarted by system, no active timer. Starting with generic notification."
            )
            startForeground(
                    GENERIC_SERVICE_NOTIFICATION_ID,
                    createGenericServiceNotification("OSOM service is active.").build()
            )
            stopSelfIfNeeded() // This will stop if truly idle
            return START_STICKY
        }

        when (action) {
            ACTION_START_TIMER -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                val requestedDuration = intent.getIntExtra(EXTRA_REQUESTED_DURATION_MINUTES, 0)
                val appUsageId = intent.getLongExtra(EXTRA_APP_USAGE_ID, -1L)

                if (appUsageId != -1L) {
                    Log.d(
                            TAG,
                            "Attempting to start timer for usage ID: $appUsageId. Initial package: $packageName, duration: $requestedDuration min"
                    )
                    // Remove any generic notification if it was shown
                    stopForeground(Service.STOP_FOREGROUND_REMOVE)
                    serviceScope.launch(Dispatchers.Main) {
                        startActiveTimerFromUsageId(appUsageId)
                    }
                } else {
                    Log.w(
                            TAG,
                            "Invalid appUsageId for START_TIMER: $appUsageId. Package: $packageName, Duration: $requestedDuration. Stopping service if needed."
                    )
                    serviceScope.launch(Dispatchers.Main) { stopSelfIfNeeded() }
                }
            }
            ACTION_CANCEL_TIMER, ACTION_USER_RETURNED -> {
                // For both explicit cancel or user returning, handle timer stop.
                // Generic notification might be needed if service needs to stay alive briefly
                val logMessageAction =
                        if (action == ACTION_CANCEL_TIMER) "ACTION_CANCEL_TIMER"
                        else "ACTION_USER_RETURNED"
                Log.d(TAG, "$logMessageAction received. Current app: $currentTimingPackageName")

                if (currentTimingPackageName != null &&
                                timerStartTime != null &&
                                action == ACTION_USER_RETURNED
                ) {
                    val timeSinceStartSeconds =
                            Duration.between(timerStartTime, LocalDateTime.now()).seconds
                    if (timeSinceStartSeconds < MIN_DURATION_BEFORE_USER_CANCEL_SECS) {
                        Log.w(
                                TAG,
                                "ACTION_USER_RETURNED ignored: too soon after timer start for $currentTimingPackageName (< $MIN_DURATION_BEFORE_USER_CANCEL_SECS s)."
                        )
                        // Do not stop the timer or its foreground notification.
                        return START_STICKY
                    }
                }
                // If not an early return, or it's a cancel action:
                handleTimerStop(userInitiatedClear = true, timerFinishedNaturally = false)
                // If service becomes idle, stopSelfIfNeeded() called in handleTimerStop will manage
                // it.
                // If it needs to stay alive without a timer (e.g. due to restart), a generic
                // notification might be required.
                // For now, assume stopSelfIfNeeded is sufficient.
            }
            else -> {
                Log.d(
                        TAG,
                        "Unknown or null action: $action. Current timer for: $currentTimingPackageName. Starting with generic notification if needed."
                )
                if (currentTimer == null
                ) { // If no active timer, and unknown action (could be system restart)
                    startForeground(
                            GENERIC_SERVICE_NOTIFICATION_ID,
                            createGenericServiceNotification("OSOM service is active.").build()
                    )
                }
                serviceScope.launch(Dispatchers.Main) { stopSelfIfNeeded() }
            }
        }
        return START_STICKY
    }

    private fun startActiveTimerFromUsageId(appUsageId: Long) {
        serviceScope.launch {
            val appUsage = appRepository.getAppUsageById(appUsageId)
            if (appUsage == null || appUsage.requestedDurationMinutes == null) {
                Log.e(
                        TAG,
                        "Could not find AppUsage for ID $appUsageId or requested duration is null. Cannot start timer."
                )
                withContext(Dispatchers.Main) { stopSelfIfNeeded() }
                return@launch
            }

            withContext(Dispatchers.Main) {
                currentTimer?.cancel()

                currentTimingPackageName = appUsage.packageName
                currentRequestedDurationMinutes = appUsage.requestedDurationMinutes
                timerStartTime = LocalDateTime.now()
                currentAppUsageId = appUsageId

                val durationMillis =
                        Duration.ofMinutes(appUsage.requestedDurationMinutes.toLong()).toMillis()

                val timedAppLabel =
                        appRepository.getAppByPackageName(appUsage.packageName)?.label
                                ?: appUsage.packageName

                Log.i(
                        TAG,
                        "Timer started on Main thread for ${appUsage.packageName}, ${appUsage.requestedDurationMinutes} min (UsageID: $appUsageId)."
                )
                val notification =
                        createTimerActiveNotification(
                                        "Timing $timedAppLabel for ${appUsage.requestedDurationMinutes} min"
                                )
                                .build()
                startForeground(TIMER_NOTIFICATION_ID, notification)

                currentTimer =
                        object : CountDownTimer(durationMillis, 1000) {
                                    override fun onTick(millisUntilFinished: Long) {
                                        // Optionally update notification with remaining time
                                    }

                                    override fun onFinish() {
                                        Log.i(
                                                TAG,
                                                "Timer naturally finished for $currentTimingPackageName (UsageID: $currentAppUsageId)"
                                        )
                                        handleTimerStop(
                                                userInitiatedClear = false,
                                                timerFinishedNaturally = true
                                        )
                                    }
                                }
                                .start()
            }
        }
    }

    private fun handleTimerStop(userInitiatedClear: Boolean, timerFinishedNaturally: Boolean) {
        val timedPackage = currentTimingPackageName
        val startTime = timerStartTime
        val reqDurationMins = currentRequestedDurationMinutes
        val usageIdToUpdate = currentAppUsageId
        val appLabelForNotification = timedPackage ?: "App"

        currentTimer?.cancel()
        currentTimer = null
        currentTimingPackageName = null
        currentRequestedDurationMinutes = null
        timerStartTime = null
        currentAppUsageId = null

        stopForeground(Service.STOP_FOREGROUND_REMOVE) // Remove active timer notification

        if (timedPackage != null &&
                        startTime != null &&
                        reqDurationMins != null &&
                        usageIdToUpdate != null
        ) {
            val actualDuration = Duration.between(startTime, LocalDateTime.now())
            val cappedActualDuration =
                    if (actualDuration.toMinutes() > reqDurationMins) {
                        Duration.ofMinutes(reqDurationMins.toLong())
                    } else {
                        actualDuration
                    }

            Log.i(
                    TAG,
                    "Timer stopped for $timedPackage. User initiated: $userInitiatedClear, Finished naturally: $timerFinishedNaturally. Actual duration: ${cappedActualDuration.seconds}s"
            )
            serviceScope.launch {
                appRepository.updateAppUsageActualDuration(usageIdToUpdate, cappedActualDuration)
                appRepository.recordUsageInUserStats(cappedActualDuration)
                if (timerFinishedNaturally) {
                    Log.d(
                            TAG,
                            "Timer finished naturally for $timedPackage. Attempting to kill and notify to bring OSOM to foreground."
                    )
                    val timedAppInfo = appRepository.getAppByPackageName(timedPackage)
                    killAppAndNotify(timedPackage, timedAppInfo?.label ?: timedPackage)
                }
            }
        } else {
            Log.w(
                    TAG,
                    "handleTimerStop called but essential timer details were null. User initiated: $userInitiatedClear, Finished naturally: $timerFinishedNaturally"
            )
        }
        serviceScope.launch(Dispatchers.Main) { stopSelfIfNeeded() }
    }

    private suspend fun killAppAndNotify(packageName: String, appLabel: String) {
        withContext(Dispatchers.IO) {
            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.killBackgroundProcesses(packageName)
                Log.i(TAG, "Attempted to kill background processes for: $packageName")
            } catch (e: SecurityException) {
                Log.e(
                        TAG,
                        "SecurityException: Cannot kill $packageName. Check KILL_BACKGROUND_PROCESSES permission.",
                        e
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error killing $packageName", e)
            }
        }

        // Post notification to bring OSOM to foreground
        val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val finishedNotification =
                createTimerFinishedNotification("$appLabel timer complete. Tap to return to OSOM.")
                        .build()
        notificationManager.notify(TIMER_FINISHED_NOTIFICATION_ID, finishedNotification)
        Log.i(TAG, "Posted timer finished notification for $appLabel")
    }

    private fun createTimerActiveNotification(contentText: String): NotificationCompat.Builder {
        // Intent to open the app itself if the "timer active" notification is tapped.
        val mainActivityIntent =
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
        val pendingIntentFlags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
        val pendingIntent =
                PendingIntent.getActivity(this, 0, mainActivityIntent, pendingIntentFlags)

        return NotificationCompat.Builder(this, TIMER_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("OSOM Timer Active")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true) // Makes it non-dismissible by swipe
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent) // Allows user to return to OSOM
    }

    private fun createTimerFinishedNotification(contentText: String): NotificationCompat.Builder {
        val intent =
                Intent(this, MainActivity::class.java).apply {
                    // Flags to bring existing OSOM to front or start new if not running
                    flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
        val pendingIntentFlags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
        val pendingIntent =
                PendingIntent.getActivity(
                        this,
                        1,
                        intent,
                        pendingIntentFlags
                ) // requestCode 1 to differentiate

        return NotificationCompat.Builder(this, TIMER_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("OSOM Timer Finished")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true) // Notification disappears when tapped
    }

    private fun createGenericServiceNotification(contentText: String): NotificationCompat.Builder {
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
        val pendingIntent =
                PendingIntent.getActivity(
                        this,
                        2,
                        mainActivityIntent,
                        pendingIntentFlags
                ) // requestCode 2

        return NotificationCompat.Builder(this, TIMER_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("OSOM Service")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true) // Keep it for foreground service state
                .setContentIntent(pendingIntent)
    }

    private fun stopSelfIfNeeded() {
        if (currentTimer == null && currentTimingPackageName == null) {
            Log.i(TAG, "No active timer. Stopping service.")
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            Log.d(TAG, "Timer still active for $currentTimingPackageName. Not stopping service.")
            // If a timer is still somehow marked as active, ensure its notification is up.
            // This might be redundant if startActiveTimerFromUsageId is robust.
            if (currentTimingPackageName != null && currentRequestedDurationMinutes != null) {
                val timedAppLabel =
                        currentTimingPackageName // Simplified, ideally fetch AppInfo label
                val notification =
                        createTimerActiveNotification(
                                        "Timing $timedAppLabel for $currentRequestedDurationMinutes min"
                                )
                                .build()
                startForeground(TIMER_NOTIFICATION_ID, notification)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        currentTimer?.cancel()
        serviceJob.cancel()
        Log.i(TAG, "AppTimerService Destroyed")
    }
}
