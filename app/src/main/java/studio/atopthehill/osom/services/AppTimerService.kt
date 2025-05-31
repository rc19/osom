package studio.atopthehill.osom.services

import android.app.* // For Service, Notification, ActivityManager, etc.
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager // Added import
import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers // Ensure Dispatchers is imported
import kotlinx.coroutines.flow.firstOrNull // Add this import
import kotlinx.coroutines.withContext // Ensure withContext is imported
import studio.atopthehill.osom.OsomApplication
import studio.atopthehill.osom.R // Ensure this is the only R import for app resources
import studio.atopthehill.osom.config.LogConfig // Import LogConfig
import studio.atopthehill.osom.data.db.entity.UsageCard
import studio.atopthehill.osom.data.repository.AppRepository
import studio.atopthehill.osom.services.OsomAccessibilityService.Companion.ACTION_CLEAR_ACCESSIBILITY_TARGET
import studio.atopthehill.osom.services.OsomAccessibilityService.Companion.ACTION_SET_ACCESSIBILITY_TARGET
import studio.atopthehill.osom.services.OsomAccessibilityService.Companion.EXTRA_PACKAGE_NAME
import studio.atopthehill.osom.utils.FileLogger // Import FileLogger
import studio.atopthehill.osom.utils.UsageStatsLogger // Import the logger

class AppTimerService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var appRepository: AppRepository
    private var currentTimer: CountDownTimer? = null
    private var currentTimingCard: UsageCard? = null
    private var timerStartTime: LocalDateTime? = null // To calculate actual elapsed time
    private var usageLogJob: Job? = null // Job for periodic usage stats logging

    private var isForegroundService = false // Track if startForeground has been called

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
        private const val USAGE_LOG_INTERVAL_MS = 30000L // Log usage every 30 seconds
        private const val USAGE_LOG_WINDOW_MS = 1000 * 60 * 5 // Log usage from last 5 minutes
    }

    override fun onCreate() {
        super.onCreate()
        appRepository = (application as OsomApplication).appRepository
        createNotificationChannel()
        Log.d(TAG, "AppTimerService Created")

        // Call startForeground immediately in onCreate with a generic notification.
        // This ensures the service complies with foreground service start requirements.
        // This notification will be updated or removed by specific timer logic later.
        val genericNotification = createNotification("OSOM Service is initializing...")
        startForeground(GENERIC_NOTIFICATION_ID, genericNotification)
        isForegroundService = true
        Log.d(TAG, "Service started in foreground with generic notification during onCreate.")

        // Initially, ensure logging is off if no session is active from a previous crash/state
        if (currentTimer == null && currentTimingCard == null) {
            LogConfig.logNotifications = false
            LogConfig.logUsageStats = false
        }
        checkForOverdueTimers()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand received: ${intent?.action}")

        val action = intent?.action

        // Ensure service is in foreground if it wasn't already (e.g. if onCreate was skipped or
        // process restarted)
        // However, since we now call it in onCreate, this might be redundant but safe.
        if (!isForegroundService) {
            val genericNotification = createNotification("OSOM Service is active.")
            startForeground(GENERIC_NOTIFICATION_ID, genericNotification)
            isForegroundService = true
            Log.w(
                    TAG,
                    "Service explicitly started in foreground from onStartCommand as it wasn't already."
            )
        }

        // Original logic for generic notification if no timer is running was here.
        // Now covered by the onCreate startForeground, but we might adjust based on action.
        if (currentTimer == null &&
                        currentTimingCard == null &&
                        (action == null || action != ACTION_START_TIMER)
        ) {
            LogConfig.logNotifications = false
            LogConfig.logUsageStats = false
            // The generic notification is already shown from onCreate or the check above.
            // We might update its text if needed, or just let it be.
            // updateNotification(GENERIC_NOTIFICATION_ID, "OSOM Service is active.")
        }

        if (action == null && currentTimer == null) { // Generic restart, check for overdue
            checkForOverdueTimers()
        }

        when (action) {
            ACTION_START_TIMER -> {
                val cardId = intent.getLongExtra(EXTRA_USAGE_CARD_ID, -1)
                if (cardId != -1L) {
                    serviceScope.launch { // This is on Dispatchers.IO
                        val usageCard = appRepository.getUsageCardById(cardId)
                        if (usageCard != null && usageCard.actualDuration == null) {
                            Log.d(TAG, "Starting timer for: ${usageCard.appName}")
                            // No need to call stopForeground(STOP_FOREGROUND_REMOVE) for generic
                            // notification
                            // because startTimerForCard will call startForeground with a new ID,
                            // replacing it.
                            // withContext(Dispatchers.Main) { startTimerForCard(usageCard) }
                            // Switch to Main dispatcher for CountDownTimer creation
                            // Call startTimerForCard directly if it manages its own context for UI
                            // parts
                            // For CountDownTimer, it MUST be on Main.
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
                // If no specific action, and not starting a timer, ensure logging is off
                if (currentTimer == null && currentTimingCard == null) {
                    LogConfig.logNotifications = false
                    LogConfig.logUsageStats = false
                }
                Log.d(TAG, "Unknown or null action. Current timer: ${currentTimingCard?.appName}")
                serviceScope.launch(Dispatchers.Main) {
                    stopSelfIfNeeded(true)
                } // True to remove generic if shown
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
                if (card.actualDuration == null &&
                                card.requestedDurationMinutes != null &&
                                card.requestedDurationMinutes > 0
                ) {
                    val expectedEndTime =
                            card.timestamp.plusMinutes(card.requestedDurationMinutes.toLong())
                    if (now.isAfter(expectedEndTime)) {
                        Log.i(
                                TAG,
                                "Found overdue timer for ${card.appName} (ID: ${card.id}). Processing."
                        )
                        // Ensure we are not trying to process a card that is currently being timed
                        // by an active CountDownTimer instance
                        if (currentTimingCard?.id != card.id) {
                            // Overdue timer means a session was active, so enable logging before
                            // processing its end
                            LogConfig.logNotifications = true
                            LogConfig.logUsageStats = true
                            sendAccessibilityTargetBroadcast(card.packageName) // For accessibility

                            // Start usage log job for this overdue card if not already running for
                            // it
                            // This is tricky because the timer itself isn't "restarted"
                            // For simplicity, we might just log one snapshot for overdue cards or
                            // rely on its original session
                            // For now, let's ensure flags are set for handleTimerCancellation to
                            // work as if session was live

                            handleTimerCancellation(
                                    userInitiated = false,
                                    appKilled = true,
                                    overdueCardId = card.id
                            )
                            processedOverdue = true
                        } else {
                            Log.d(
                                    TAG,
                                    "Overdue card ${card.appName} is current live timer. Letting CountDown handle it."
                            )
                        }
                    }
                }
            }
            // If no timers are active after checking overdue, ensure logs are off.
            if (!processedOverdue && currentTimer == null && currentTimingCard == null) {
                LogConfig.logNotifications = false
                LogConfig.logUsageStats = false
            }
        }
    }

    private fun startTimerForCard(usageCard: UsageCard) {
        currentTimer?.cancel()
        usageLogJob?.cancel() // Cancel any existing usage log job

        // Start file logging session needs to happen before timerStartTime might be nulled by a
        // quick stop
        val localTimerStartTime = LocalDateTime.now()
        currentTimingCard = usageCard
        timerStartTime = localTimerStartTime // Set timerStartTime here

        FileLogger.startSession(this, usageCard.appName, timerStartTime!!)

        val requestedMinutes = usageCard.requestedDurationMinutes
        if (requestedMinutes == null || requestedMinutes <= 0) {
            Log.e(
                    TAG,
                    "Cannot start timer for ${usageCard.appName}, requested duration is null or invalid: $requestedMinutes"
            )
            // Send broadcast to clear accessibility target if setup failed before timer start
            sendAccessibilityTargetBroadcast(null)
            LogConfig.logNotifications = false // Ensure logs off if start fails
            LogConfig.logUsageStats = false
            stopSelfIfNeeded(true)
            return
        }

        // Enable logging for this session
        LogConfig.logNotifications = true
        LogConfig.logUsageStats = true
        sendAccessibilityTargetBroadcast(
                usageCard.packageName
        ) // For targeted accessibility logging

        usageLogJob =
                serviceScope.launch {
                    Log.d(
                            TAG,
                            "Starting periodic global usage & notification logging for session: ${usageCard.packageName}"
                    )
                    while (isActive &&
                            currentTimingCard?.id ==
                                    usageCard.id) { // Loop while this card is active
                        UsageStatsLogger.logRecentUsageStats(
                                context = this@AppTimerService,
                                durationMillis = USAGE_LOG_WINDOW_MS.toLong()
                        )
                        // Log to file that a batch of usage stats was processed
                        FileLogger.log(TAG, "Periodic usage stats batch logged to Logcat.")
                        delay(USAGE_LOG_INTERVAL_MS)
                    }
                    Log.d(
                            TAG,
                            "Stopped periodic global usage & notification logging for session: ${usageCard.packageName}"
                    )
                }

        val durationMillis = java.time.Duration.ofMinutes(requestedMinutes.toLong()).toMillis()

        val notification =
                createNotification("Timing ${usageCard.appName} for $requestedMinutes min")
        startForeground(NOTIFICATION_ID, notification) // This will update/replace the generic one
        isForegroundService = true // Confirm it's in foreground state
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
        usageLogJob?.cancel() // Always cancel the usage log job first
        usageLogJob = null

        // Determine the session end time
        val sessionEndTime = LocalDateTime.now()

        // End file logging session before clearing currentTimingCard info
        // Pass the determined sessionEndTime and context.
        // If handling an overdue card, its original start time is fetched from DB later if
        // currentTimingCard is null.
        // However, FileLogger relies on its internally stored start time.
        // This assumes FileLogger.startSession was called correctly when the *live* timer started.
        // For overdue cards not live, file logging might not be active here unless re-initiated.
        // For simplicity, we only end a session if one was actively started by this service
        // instance.
        if (currentTimingCard != null || overdueCardId != null
        ) { // Check if there was any kind of session to end
            FileLogger.endSession(sessionEndTime)
        }

        // Disable logging as the session is ending
        LogConfig.logNotifications = false
        LogConfig.logUsageStats = false

        val cardIdToProcess = overdueCardId ?: currentTimingCard?.id
        val isProcessingCurrentLiveTimer =
                overdueCardId == null || overdueCardId == currentTimingCard?.id

        // Clear accessibility target regardless of which card is processed
        // If this cancellation means no app is being actively tracked by timer service.
        if (isProcessingCurrentLiveTimer || overdueCardId != null
        ) { // Ensure we send clear if any timer-related activity ceases
            sendAccessibilityTargetBroadcast(null)
        }

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
                                if (startTimeForCalc != null) {
                                    Duration.between(startTimeForCalc, LocalDateTime.now())
                                } else {
                                    Duration.ofMinutes(
                                            updatedCard.requestedDurationMinutes?.toLong() ?: 0L
                                    )
                                }
                        updatedCard.actualDuration = calculatedActualDuration
                        appRepository.updateUsageCard(updatedCard)
                        Log.d(
                                TAG,
                                "Updated UsageCard: ${updatedCard.packageName} with actual duration: $calculatedActualDuration (User Initiated: $userInitiated, AppKilled: $appKilled)"
                        )
                        // Add the actual duration to total user usage stats
                        if (calculatedActualDuration > Duration.ZERO
                        ) { // Only add if positive duration
                            appRepository.addDurationToTotalUsage(calculatedActualDuration)
                        }
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
            // This case implies no current live timer was affected, but an overdue one might be.
            // If an overdue card was processed, sendAccessibilityTargetBroadcast(null) above
            // already handled it.
            // If no cardIdToProcess (e.g., service was just started and stopped without any card
            // logic), also ensure target is cleared.
            if (cardIdToProcess == null) {
                sendAccessibilityTargetBroadcast(null)
            }
            stopSelfIfNeeded() // If no card was being timed, still check if service should stop
        }
    }

    private fun sendAccessibilityTargetBroadcast(packageName: String?) {
        val intent = Intent()
        if (packageName != null) {
            intent.action = ACTION_SET_ACCESSIBILITY_TARGET
            intent.putExtra(EXTRA_PACKAGE_NAME, packageName)
            Log.d(
                    TAG,
                    "Sending broadcast to set accessibility target for AccessibilityService: $packageName"
            )
        } else {
            intent.action = ACTION_CLEAR_ACCESSIBILITY_TARGET
            Log.d(TAG, "Sending broadcast to clear accessibility target for AccessibilityService")
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
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
            // No need to explicitly call stopForeground(STOP_FOREGROUND_REMOVE) if
            // removeGenericNotification is true
            // because stopSelf() will remove notifications if the service was started with
            // startForeground.
            // However, to be explicit about removing a *specific* generic notification if it was
            // shown
            // and no timer notification took over, we can do this:
            if (removeGenericNotification) {
                stopForeground(Service.STOP_FOREGROUND_REMOVE) // Explicitly remove notification
                Log.d(TAG, "Generic notification removed explicitly.")
            }
            // else, if a timer was active, its notification (NOTIFICATION_ID) would be active.
            // stopSelf() should handle removing that.

            // Before stopping, ensure logs are off if this is an unexpected stop
            LogConfig.logNotifications = false
            LogConfig.logUsageStats = false
            stopSelf()
            isForegroundService = false // Mark as no longer foreground
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
        usageLogJob?.cancel() // Ensure job is cancelled on service destroy
        currentTimer?.cancel()

        FileLogger.endSession(LocalDateTime.now())

        serviceJob.cancel()
        LogConfig.logNotifications = false
        LogConfig.logUsageStats = false
        // Explicitly call stopForeground if it might still be considered in foreground state by the
        // system
        // although stopSelf() should also handle this.
        if (isForegroundService) {
            stopForeground(true) // true = remove notification
            isForegroundService = false
            Log.d(TAG, "Service stopped from onDestroy, explicitly called stopForeground.")
        }
        Log.d(TAG, "AppTimerService Destroyed")
    }
}
