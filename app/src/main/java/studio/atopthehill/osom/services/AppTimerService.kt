package studio.atopthehill.osom.services

import android.app.* // For Service, Notification, ActivityManager, etc.
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
import kotlinx.coroutines.flow.firstOrNull // Add this import
import kotlinx.coroutines.withContext // Ensure withContext is imported
import studio.atopthehill.osom.OsomApplication
import studio.atopthehill.osom.R // Ensure this is the only R import for app resources
import studio.atopthehill.osom.data.db.entity.UsageCard
import studio.atopthehill.osom.data.repository.AppRepository

class AppTimerService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var appRepository: AppRepository
    private var currentTimer: CountDownTimer? = null
    private var currentTimingCard: UsageCard? = null
    private var timerStartTime: LocalDateTime? = null // To calculate actual elapsed time

    companion object {
        const val ACTION_START_TIMER = "studio.atopthehill.osom.services.action.START_TIMER"
        const val ACTION_CANCEL_TIMER =
                "studio.atopthehill.osom.services.action.CANCEL_TIMER" // Can be used if user
        // explicitly cancels a card
        // from UI
        const val ACTION_USER_RETURNED = "studio.atopthehill.osom.services.action.USER_RETURNED"
        const val EXTRA_USAGE_CARD_ID = "studio.atopthehill.osom.services.extra.USAGE_CARD_ID"
        private const val NOTIFICATION_CHANNEL_ID = "AppTimerServiceChannel"
        private const val NOTIFICATION_ID = 1868 // OSOM ASCII :)
        private const val GENERIC_NOTIFICATION_ID = 1869 // Different ID for generic notification
        private const val TAG = "AppTimerService"
        private const val MIN_DURATION_BEFORE_USER_CANCEL_SECS = 5L // 5 seconds
    }

    override fun onCreate() {
        super.onCreate()
        appRepository = (application as OsomApplication).appRepository
        createNotificationChannel()
        Log.d(TAG, "AppTimerService Created")
        // Check for overdue timers on service creation (e.g. after crash recovery)
        checkForOverdueTimers()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand received: ${intent?.action}")

        val action = intent?.action

        if (currentTimer == null && (action == ACTION_USER_RETURNED || action == null)) {
            val genericNotification = createNotification("OSOM Service is active.")
            startForeground(GENERIC_NOTIFICATION_ID, genericNotification)
            // If it's a restart (action == null), check for overdue timers explicitly
            if (action == null) {
                checkForOverdueTimers()
            }
        }

        when (action) {
            ACTION_START_TIMER -> {
                val cardId = intent.getLongExtra(EXTRA_USAGE_CARD_ID, -1)
                if (cardId != -1L) {
                    serviceScope.launch { // This is on Dispatchers.IO
                        val usageCard = appRepository.getUsageCardById(cardId)
                        if (usageCard != null && usageCard.actualDuration == null) {
                            Log.d(TAG, "Starting timer for: ${usageCard.appName}")
                            stopForeground(
                                    Service.STOP_FOREGROUND_REMOVE
                            ) // Remove generic notification
                            // Switch to Main dispatcher for CountDownTimer creation
                            withContext(Dispatchers.Main) { startTimerForCard(usageCard) }
                        } else {
                            Log.w(TAG, "UsageCard not found or already completed for id: $cardId")
                            withContext(
                                    Dispatchers.Main
                            ) { // stopSelfIfNeeded might interact with service lifecycle
                                stopSelfIfNeeded(true)
                            }
                        }
                    }
                } else {
                    serviceScope.launch(
                            Dispatchers.Main
                    ) { // Ensure stopSelfIfNeeded is on Main if it touches lifecycle directly
                        stopSelfIfNeeded(true)
                    }
                }
            }
            ACTION_CANCEL_TIMER -> {
                Log.d(TAG, "ACTION_CANCEL_TIMER received for: ${currentTimingCard?.appName}")
                // handleTimerCancellation involves DB ops (IO) and UI ops (Main for
                // bringToForeground)
                // It manages its own context switching internally or its sub-calls do.
                // For safety, the initial call from onStartCommand can be wrapped if needed, but
                // CountDownTimer is main concern.
                handleTimerCancellation(userInitiated = true, appKilled = false)
            }
            ACTION_USER_RETURNED -> {
                Log.d(
                        TAG,
                        "ACTION_USER_RETURNED received while timing: ${currentTimingCard?.appName}"
                )
                val timeSinceStartSeconds =
                        if (timerStartTime != null && currentTimingCard != null) {
                            Duration.between(timerStartTime, LocalDateTime.now()).seconds
                        } else {
                            MIN_DURATION_BEFORE_USER_CANCEL_SECS +
                                    1 // Ensure it doesn't trigger ignore if no timer was active
                        }

                if (currentTimingCard != null &&
                                timeSinceStartSeconds < MIN_DURATION_BEFORE_USER_CANCEL_SECS
                ) {
                    Log.w(
                            TAG,
                            "ACTION_USER_RETURNED received within ${MIN_DURATION_BEFORE_USER_CANCEL_SECS}s of timer start for ${currentTimingCard?.appName}. Ignoring to prevent premature cancellation."
                    )
                    // If generic notification was shown because currentTimer became null briefly,
                    // ensure it's handled.
                    // This path implies currentTimingCard IS NOT NULL, so a specific timer
                    // notification should be active.
                    // No explicit stopForeground/stopSelf here; let the active timer continue.
                } else {
                    if (currentTimer == null) { // No active timer in this service instance
                        checkForOverdueTimers() // Check if any persisted timers are overdue
                    }
                    // Proceed with normal cancellation as user has likely genuinely returned or an
                    // overdue timer needs processing.
                    handleTimerCancellation(userInitiated = true, appKilled = false)
                }
            }
            else -> {
                Log.d(TAG, "Unknown or null action. Current timer: ${currentTimingCard?.appName}")
                serviceScope.launch(Dispatchers.Main) { stopSelfIfNeeded(true) }
            }
        }
        return START_STICKY
    }

    private fun checkForOverdueTimers() {
        serviceScope.launch {
            Log.d(TAG, "Checking for overdue timers...")
            val now = LocalDateTime.now()
            val activeCardsList: List<UsageCard> =
                    appRepository.getActiveUsageCards().firstOrNull() ?: emptyList()
            var processedOverdue = false
            activeCardsList.forEach { card ->
                if (card.actualDuration == null) {
                    val expectedEndTime =
                            card.timestamp.plusMinutes(card.requestedDurationMinutes.toLong())
                    if (now.isAfter(expectedEndTime)) {
                        Log.i(
                                TAG,
                                "Found overdue timer for ${card.appName} (ID: ${card.id}). Processing as finished."
                        )
                        // Ensure we are not trying to process a card that is currently being timed
                        // by an active CountDownTimer instance
                        if (currentTimingCard?.id != card.id) {
                            handleTimerCancellation(
                                    userInitiated = false,
                                    appKilled = true,
                                    overdueCardId = card.id
                            )
                            processedOverdue = true
                        } else {
                            Log.d(
                                    TAG,
                                    "Overdue card ${card.appName} is the current live timer. Letting CountDownTimer handle it."
                            )
                        }
                    }
                }
            }
            if (processedOverdue && currentTimer == null) {
                // If we processed overdue cards and there's no active live timer, the service might
                // be able to stop.
                // stopSelfIfNeeded() will be called by handleTimerCancellation.
            } else if (!processedOverdue && currentTimer == null && currentTimingCard == null) {
                // If no overdue cards were processed and no active timer, and service was started
                // generically.
                // This case might already be handled by stopSelfIfNeeded in onStartCommand's else
                // block
                // or if called after generic notification without a specific action.
            }
        }
    }

    private fun startTimerForCard(usageCard: UsageCard) {
        currentTimer?.cancel()
        currentTimingCard = usageCard
        timerStartTime = LocalDateTime.now()
        val durationMillis =
                java.time.Duration.ofMinutes(usageCard.requestedDurationMinutes.toLong()).toMillis()

        val notification =
                createNotification(
                        "Timing ${usageCard.appName} for ${usageCard.requestedDurationMinutes} min"
                )
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "Timer started on Main thread for ${usageCard.appName}")

        // CountDownTimer must be created and started on a thread with a Looper.
        // Since this function is now called via withContext(Dispatchers.Main),
        // this will execute on the main thread.
        currentTimer =
                object : CountDownTimer(durationMillis, 1000) {
                            override fun onTick(millisUntilFinished: Long) {}
                            override fun onFinish() {
                                Log.d(
                                        TAG,
                                        "Timer naturally finished for ${currentTimingCard?.appName}"
                                )
                                // handleTimerCancellation will do DB work on IO, bringToForeground
                                // on Main
                                serviceScope.launch {
                                    handleTimerCancellation(userInitiated = false, appKilled = true)
                                }
                            }
                        }
                        .start()
    }

    /**
     * Handles the logic for when a timer stops, either naturally or by interruption.
     * @param userInitiated If true, the stop was due to user action (returned to OSOM, or explicit
     * cancel).
     * @param appKilled If true, indicates an attempt should be/was made to kill the timed app
     * (usually when timer finishes naturally).
     */
    private fun handleTimerCancellation(
            userInitiated: Boolean,
            appKilled: Boolean,
            overdueCardId: Long? = null
    ) {
        val cardIdToProcess = overdueCardId ?: currentTimingCard?.id
        val isProcessingCurrentLiveTimer =
                overdueCardId == null || overdueCardId == currentTimingCard?.id

        if (isProcessingCurrentLiveTimer) {
            currentTimer?.cancel()
            currentTimer = null
            currentTimingCard = null
            timerStartTime = null
        }
        // Always remove notification if one was shown (either generic or specific)
        stopForeground(Service.STOP_FOREGROUND_REMOVE)

        if (cardIdToProcess != null) {
            serviceScope.launch {
                val cardToUpdate = appRepository.getUsageCardById(cardIdToProcess)
                val startTimeForCalc =
                        if (isProcessingCurrentLiveTimer) timerStartTime
                        else cardToUpdate?.timestamp

                cardToUpdate?.let { updatedCard ->
                    // Only update actualDuration if it hasn't been set yet (relevant for overdue
                    // processing)
                    if (updatedCard.actualDuration == null) {
                        val calculatedActualDuration =
                                if (startTimeForCalc != null && isProcessingCurrentLiveTimer) {
                                    Duration.between(startTimeForCalc, LocalDateTime.now())
                                } else {
                                    // For overdue cards, or if startTime was lost, calculate from
                                    // requested duration
                                    Duration.ofMinutes(
                                            updatedCard.requestedDurationMinutes.toLong()
                                    )
                                }
                        updatedCard.actualDuration = calculatedActualDuration
                        appRepository.updateUsageCard(updatedCard)
                        Log.d(
                                TAG,
                                "Updated UsageCard: ${updatedCard.packageName} with actual duration: $calculatedActualDuration (User Initiated: $userInitiated, AppKilled: $appKilled)"
                        )
                    }

                    if (appKilled && !userInitiated) {
                        val activityManager =
                                getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                        try {
                            Log.i(
                                    TAG,
                                    "Attempting to kill background processes for: ${updatedCard.packageName}"
                            )
                            activityManager.killBackgroundProcesses(updatedCard.packageName)
                            bringOsomToForeground()
                        } catch (e: Exception) {
                            Log.e(
                                    TAG,
                                    "Error during post-timer actions for ${updatedCard.packageName}",
                                    e
                            )
                            bringOsomToForeground()
                        }
                    } else if (userInitiated) {
                        Log.d(
                                TAG,
                                "Timer for ${updatedCard.packageName} (ID: ${updatedCard.id}) stopped by user action. App not killed by service."
                        )
                        // If user returned to OSOM, OSOM is already in foreground.
                    }
                }
                stopSelfIfNeeded()
            }
        } else {
            stopSelfIfNeeded() // If no card was being timed, still check if service should stop
        }
    }

    private fun bringOsomToForeground() {
        val mainActivityName = "studio.atopthehill.osom.MainActivity"
        try {
            val mainActivityClass = Class.forName(mainActivityName)
            val intent =
                    Intent(applicationContext, mainActivityClass).apply {
                        action = Intent.ACTION_MAIN
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        )
                    }
            // startActivity is safe to call from non-UI threads.
            applicationContext.startActivity(intent)
            Log.i(TAG, "Brought OSOM to foreground.")
        } catch (e: ClassNotFoundException) {
            Log.e(
                    TAG,
                    "MainActivity class not found: $mainActivityName. Cannot bring OSOM to foreground.",
                    e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error bringing OSOM to foreground", e)
        }
    }

    private fun stopSelfIfNeeded(removeGenericNotification: Boolean = false) {
        if (currentTimer == null && currentTimingCard == null) {
            Log.d(TAG, "No active timer, stopping service.")
            if (removeGenericNotification) {
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
            }
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel =
                    NotificationChannel(
                            NOTIFICATION_CHANNEL_ID,
                            "App Timer Service Channel",
                            NotificationManager.IMPORTANCE_LOW
                    )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("OSOM App Timer")
                .setContentText(contentText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        currentTimer?.cancel()
        serviceJob.cancel()
        Log.d(TAG, "AppTimerService Destroyed")
    }
}
