package studio.atopthehill.osom.data.repository // Package declaration for the repository

// AppUsageDao import will be removed by tool if not used
// AppUsage entity import will be removed by tool if not used
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context // Android Context for accessing system services like PackageManager
import android.content.Intent // Intent for querying launchable apps
import android.graphics.Bitmap // Bitmap for handling app icons
import android.graphics.drawable.BitmapDrawable // Drawable to Bitmap conversion
import android.os.Build
import android.util.Log
import androidx.core.graphics.drawable.toBitmap // Extension function for Drawable to Bitmap
import java.io.ByteArrayOutputStream // For converting Bitmap to ByteArray
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlinx.coroutines.Dispatchers // Coroutine dispatcher for IO operations
import kotlinx.coroutines.flow.Flow // Kotlin Coroutines Flow for asynchronous data streams
import kotlinx.coroutines.flow.firstOrNull // Added for Flow.firstOrNull()
import kotlinx.coroutines.withContext // Coroutine scope for switching dispatchers
import studio.atopthehill.osom.data.db.AppDatabase // The Room database instance
import studio.atopthehill.osom.data.db.dao.AppInfoDao // DAO for AppInfo entity
import studio.atopthehill.osom.data.db.dao.UsageCardDao
import studio.atopthehill.osom.data.db.dao.UserStatsDao
import studio.atopthehill.osom.data.db.entity.AppInfo // AppInfo entity
import studio.atopthehill.osom.data.db.entity.UsageCard
import studio.atopthehill.osom.data.db.entity.UserStats
import studio.atopthehill.osom.data.db.entity.TaskStatus

class AppRepository(
        private val context: Context,
        private val appInfoDao: AppInfoDao,
        private val usageCardDao: UsageCardDao,
        private val userStatsDao: UserStatsDao
) { // Repository class, takes Context for PackageManager


        private val TAG = "AppRepositoryUsageLogs"

        // --- AppInfo Operations ---

        val allInstalledApps: Flow<List<AppInfo>> =
                appInfoDao.getInstalledApps() // Flow of all installed apps

        suspend fun refreshInstalledApps() { // Function to refresh the list of installed apps in
                // the
                // database
                withContext(Dispatchers.IO) { // Perform in IO context
                        val existingApps = appInfoDao.getAllApps().firstOrNull()?.associateBy { it.packageName } ?: emptyMap()

                        val packageManager = context.packageManager // Get PackageManager
                        val intent =
                                Intent(Intent.ACTION_MAIN, null)
                                        .apply { // Intent to find launchable apps
                                                addCategory(Intent.CATEGORY_LAUNCHER)
                                        }
                        val resolveInfoList =
                                packageManager.queryIntentActivities(
                                        intent,
                                        0
                                ) // Get list of launchable activities

                        val apps =
                                resolveInfoList.mapNotNull { resolveInfo
                                        -> // Map to AppInfo objects
                                        val packageName = resolveInfo.activityInfo.packageName
                                        val label = resolveInfo.loadLabel(packageManager).toString()
                                        val iconDrawable = resolveInfo.loadIcon(packageManager)

                                        // Convert icon to ByteArray (or handle if not needed)
                                        val iconByteArray =
                                                try {
                                                        val bitmap =
                                                                if (iconDrawable is BitmapDrawable
                                                                ) {
                                                                        iconDrawable.bitmap
                                                                } else {
                                                                        iconDrawable.toBitmap(
                                                                                100,
                                                                                100,
                                                                                Bitmap.Config
                                                                                        .ARGB_8888
                                                                        )
                                                                }
                                                        val stream = ByteArrayOutputStream()
                                                        bitmap.compress(
                                                                Bitmap.CompressFormat.PNG,
                                                                90,
                                                                stream
                                                        ) // Compress bitmap to PNG
                                                        stream.toByteArray() // Get byte array
                                                } catch (e: Exception) {
                                                        // Log error or handle missing/corrupt icon
                                                        null
                                                }

                                        val existingApp = existingApps[packageName]

                                        AppInfo(
                                                packageName,
                                                label,
                                                iconByteArray,
                                                isInstalled = true,
                                                lastUpdated = System.currentTimeMillis(),
                                                isWhitelisted = existingApp?.isWhitelisted ?: false
                                        )
                                }
                        appInfoDao.insertOrUpdateAllApps(apps) // Insert or update all fetched apps

                        // Optionally, mark apps not in resolveInfoList as uninstalled (more complex
                        // logic
                        // needed here to compare with existing DB)
                }
        }

        suspend fun getAppByPackageName(
                packageName: String
        ): AppInfo? { // Get a specific app by package name
                return appInfoDao.getAppByPackageName(packageName)
        }

        suspend fun markAppAsUninstalled(packageName: String) { // Mark an app as uninstalled
                appInfoDao.markAsUninstalled(packageName)
        }

        suspend fun insertOrUpdateApp(appInfo: AppInfo) { // Insert or update a single app
                appInfoDao.insertOrUpdateApp(appInfo)
        }

        suspend fun setWhitelisted(packageName: String, isWhitelisted: Boolean) {
                appInfoDao.setWhitelisted(packageName, isWhitelisted)
        }

        // --- AppUsage (Old - to be reviewed if still needed or replaced by UsageCard) --- REMOVED
        // SECTION

        // --- UsageCard Operations ---
        fun getAllUsageCards(): Flow<List<UsageCard>> = usageCardDao.getAllUsageCards()

        fun getUsageCardsForDay(day: LocalDateTime): Flow<List<UsageCard>> {
                val dateString =
                        day.toLocalDate()
                                .format(
                                        java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
                                ) // YYYY-MM-DD
                return usageCardDao.getUsageCardsForDay(dateString)
        }

        fun getPendingTasks(): Flow<List<UsageCard>> = usageCardDao.getPendingTasks(LocalDateTime.now())

        fun getCompletedTasks(): Flow<List<UsageCard>> = usageCardDao.getCompletedTasks()

        fun getActiveUsageCards(): Flow<List<UsageCard>> = usageCardDao.getActiveUsageCards()

        suspend fun insertUsageCard(usageCard: UsageCard): Long {
                return usageCardDao.insertUsageCard(usageCard)
        }

        suspend fun updateUsageCard(usageCard: UsageCard) {
                usageCardDao.updateUsageCard(usageCard)
        }

        suspend fun getUsageCardById(id: Long): UsageCard? {
                return usageCardDao.getUsageCardById(id)
        }

        suspend fun updateTaskStatus(id: Long, status: TaskStatus) {
                usageCardDao.updateTaskStatus(id, status.name)
        }

        suspend fun updateTaskSnoozeStatus(id: Long, status: TaskStatus, snoozeUntil: LocalDateTime?) {
                usageCardDao.updateTaskSnoozeStatus(id, status.name, snoozeUntil)
        }

        suspend fun updateTaskCompletion(id: Long, status: TaskStatus, completionTimestamp: LocalDateTime?) {
                usageCardDao.updateTaskCompletion(id, status.name, completionTimestamp)
        }

        suspend fun updateTaskTimestamp(id: Long, timestamp: LocalDateTime) {
                usageCardDao.updateTaskTimestamp(id, timestamp)
        }

        suspend fun deleteUsageCardById(id: Long) {
                usageCardDao.deleteUsageCardById(id)
        }

        suspend fun getLatestUsageCardForPackage(packageName: String): UsageCard? {
                return usageCardDao.getLatestUsageCardForPackage(packageName)
        }

        // --- UserStats Operations ---
        fun getUserStats(): Flow<UserStats?> = userStatsDao.getUserStats()

        suspend fun getUserStatsDirect(): UserStats? = userStatsDao.getUserStatsDirect()

        suspend fun insertOrUpdateUserStats(userStats: UserStats) {
                userStatsDao.insertOrUpdateStats(userStats)
        }

        suspend fun initializeDefaultUserStatsIfNeeded() {
                withContext(Dispatchers.IO) {
                        if (userStatsDao.getUserStatsDirect() == null) {
                                val defaultStats =
                                        UserStats(
                                                dailyInteractions = 0,
                                                totalUsageToday = Duration.ZERO,
                                                lastInteraction = LocalDateTime.now(),
                                                userName = "Ketan", // Set default username
                                                dayStartTimeHour = 3 // Default start hour
                                        )
                                userStatsDao.insertOrUpdateStats(defaultStats)
                        }
                }
        }

        suspend fun addDurationToTotalUsage(duration: Duration) {
                withContext(Dispatchers.IO) {
                        val currentStats = userStatsDao.getUserStatsDirect()
                        if (currentStats != null) {
                                val newTotalUsage = currentStats.totalUsageToday.plus(duration)
                                userStatsDao.insertOrUpdateStats(
                                        currentStats.copy(totalUsageToday = newTotalUsage)
                                )
                        }
                }
        }

        // AI Tasks setting methods for MVP LLM integration
        suspend fun setAITasksEnabled(enabled: Boolean) {
                withContext(Dispatchers.IO) {
                        userStatsDao.setAITasksEnabled(enabled)
                }
        }

        suspend fun isAITasksEnabled(): Boolean {
                return withContext(Dispatchers.IO) {
                        userStatsDao.getUserStatsDirect()?.enableAITasks ?: false
                }
        }

        // --- Package Usage Stats Logging ---
        /* Commenting out as this is being consolidated into utils.UsageStatsLogger
        @Suppress("SimpleDateFormat") // Using for specific log format
        suspend fun logRecentUsageEvents() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                        withContext(Dispatchers.IO) {
                                val usageStatsManager =
                                        context.getSystemService(Context.USAGE_STATS_SERVICE) as
                                                UsageStatsManager
                                val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

                                val endTime = System.currentTimeMillis()
                                val beginTime = endTime - (60 * 60 * 1000) // Last 1 hour

                                Log.d(
                                        TAG,
                                        "Querying usage events from ${timeFormat.format(Date(beginTime))} to ${timeFormat.format(Date(endTime))}"
                                )

                                val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
                                if (usageEvents == null) {
                                        Log.d(
                                                TAG,
                                                "Usage events are null. Check permissions or if data is available."
                                        )
                                        return@withContext
                                }

                                val event = UsageEvents.Event() // Reusable event object
                                var eventCount = 0
                                while (usageEvents.hasNextEvent()) {
                                        usageEvents.getNextEvent(event)
                                        eventCount++
                                        val eventTypeString =
                                                when (event.eventType) {
                                                        UsageEvents.Event.ACTIVITY_RESUMED ->
                                                                "ACTIVITY_RESUMED (FG)" // Deprecated but often available
                                                        UsageEvents.Event.ACTIVITY_PAUSED ->
                                                                "ACTIVITY_PAUSED (BG)"
                                                        UsageEvents.Event.ACTIVITY_STOPPED ->
                                                                "ACTIVITY_STOPPED"
                                                        UsageEvents.Event.CONFIGURATION_CHANGE ->
                                                                "CONFIGURATION_CHANGE"
                                                        UsageEvents.Event.DEVICE_SHUTDOWN ->
                                                                "DEVICE_SHUTDOWN"
                                                        UsageEvents.Event.DEVICE_STARTUP ->
                                                                "DEVICE_STARTUP"
                                                        UsageEvents.Event
                                                                .FOREGROUND_SERVICE_START ->
                                                                "FG_SERVICE_START"
                                                        UsageEvents.Event.FOREGROUND_SERVICE_STOP ->
                                                                "FG_SERVICE_STOP"
                                                        UsageEvents.Event.KEYGUARD_HIDDEN ->
                                                                "KEYGUARD_HIDDEN (Unlocked)"
                                                        UsageEvents.Event.KEYGUARD_SHOWN ->
                                                                "KEYGUARD_SHOWN (Locked)"
                                                        UsageEvents.Event.SCREEN_INTERACTIVE ->
                                                                "SCREEN_INTERACTIVE (Screen ON)"
                                                        UsageEvents.Event.SCREEN_NON_INTERACTIVE ->
                                                                "SCREEN_NON_INTERACTIVE (Screen OFF)"
                                                        UsageEvents.Event.SHORTCUT_INVOCATION ->
                                                                "SHORTCUT_INVOCATION"
                                                        UsageEvents.Event.STANDBY_BUCKET_CHANGED ->
                                                                "STANDBY_BUCKET_CHANGED"
                                                        UsageEvents.Event.USER_INTERACTION ->
                                                                "USER_INTERACTION"
                                                        //  UsageEvents.Event
                                                        //          .NOTIFICATION_INTERRUPTION ->
                                                        //          "NOTIFICATION_INTERRUPTION"
                                                        // UsageEvents.Event.SLICE_PINNED_PRIV is
                                                        // API 28+
                                                        // UsageEvents.Event.SYSTEM_INTERACTION is
                                                        // API 28+
                                                        // UsageEvents.Event.USER_STOPPED is API 29+
                                                        // UsageEvents.Event.PACKAGE_UNINSTALLED is
                                                        // API 29+
                                                        // UsageEvents.Event.PACKAGE_REPLACED is API
                                                        // 29+
                                                        else -> "UNKNOWN_EVENT (${event.eventType})"
                                                }
                                        Log.d(
                                                TAG,
                                                "Event: ${event.packageName}, Type: $eventTypeString, Time: ${timeFormat.format(Date(event.timeStamp))}"
                                        )
                                }
                                if (eventCount == 0) {
                                        Log.d(
                                                TAG,
                                                "No usage events found in the last hour. Ensure OSOM has PACKAGE_USAGE_STATS permission granted in system settings (Usage access)."
                                        )
                                } else {
                                        Log.d(TAG, "Finished logging $eventCount usage events.")
                                }
                        }
                } else {
                        Log.w(TAG, "UsageStatsManager not available on this API level.")
                }
        }
        */

        // --- Usage Card Backfilling ---
        @Suppress("SimpleDateFormat")
        suspend fun backfillUsageCardsFromUsageStats(day: LocalDateTime) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                        Log.w(
                                TAG,
                                "UsageStatsManager not available for backfilling on this API level."
                        )
                        return
                }

                withContext(Dispatchers.IO) {
                        // Check if usage cards already exist for this day
                        val dateString =
                                day.toLocalDate()
                                        .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                        val existingCardsForDay =
                                usageCardDao.getUsageCardsForDay(dateString).firstOrNull()
                        if (!existingCardsForDay.isNullOrEmpty()) {
                                Log.d(
                                        TAG,
                                        "[Backfill] UsageCards already exist for ${day.toLocalDate()}. Skipping backfill."
                                )
                                return@withContext
                        }

                        val usageStatsManager =
                                context.getSystemService(Context.USAGE_STATS_SERVICE) as
                                        UsageStatsManager
                        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss") // For logging

                        val startOfDay =
                                day.toLocalDate()
                                        .atStartOfDay(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli()
                        val endOfDay =
                                day.toLocalDate()
                                        .plusDays(1)
                                        .atStartOfDay(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli() - 1

                        Log.d(
                                TAG,
                                "Backfilling UsageCards for ${day.toLocalDate()} from ${timeFormat.format(Date(startOfDay))} to ${timeFormat.format(Date(endOfDay))}"
                        )

                        val usageEvents = usageStatsManager.queryEvents(startOfDay, endOfDay)
                        if (usageEvents == null) {
                                Log.d(
                                        TAG,
                                        "Usage events for backfill are null. Check permissions or if data is available."
                                )
                                return@withContext
                        }

                        val appSessions =
                                mutableMapOf<
                                        String, Long>() // packageName to sessionStartTimeMillis
                        val processedSessionTimestamps =
                                mutableSetOf<
                                        Long>() // To avoid double processing very close sessions

                        val osomPackageName = context.packageName
                        val event = UsageEvents.Event() // Reusable event object

                        while (usageEvents.hasNextEvent()) {
                                usageEvents.getNextEvent(event)

                                if (event.packageName == null ||
                                                event.packageName == osomPackageName
                                ) {
                                        continue // Skip OSOM events or events with no package name
                                }

                                when (event.eventType) {
                                        UsageEvents.Event.ACTIVITY_RESUMED -> {
                                                // If another app was active, consider its session
                                                // ended just before this one started.
                                                val currentlyTrackedApp =
                                                        appSessions.keys.firstOrNull()
                                                if (currentlyTrackedApp != null &&
                                                                currentlyTrackedApp !=
                                                                        event.packageName
                                                ) {
                                                        val sessionStartTimeMillis =
                                                                appSessions.remove(
                                                                        currentlyTrackedApp
                                                                )
                                                        if (sessionStartTimeMillis != null) {
                                                                val sessionEndTimeMillis =
                                                                        event.timeStamp -
                                                                                1 // End just before
                                                                // new app resumed
                                                                createCardFromSession(
                                                                        currentlyTrackedApp,
                                                                        sessionStartTimeMillis,
                                                                        sessionEndTimeMillis,
                                                                        processedSessionTimestamps
                                                                )
                                                        }
                                                }
                                                // Start or update session for the current app
                                                if (!appSessions.containsKey(event.packageName)) {
                                                        appSessions[event.packageName] =
                                                                event.timeStamp
                                                        Log.d(
                                                                TAG,
                                                                "[Backfill] RESUMED: ${event.packageName} at ${timeFormat.format(Date(event.timeStamp))}"
                                                        )
                                                }
                                        }
                                        UsageEvents.Event.ACTIVITY_PAUSED,
                                        UsageEvents.Event.ACTIVITY_STOPPED -> {
                                                if (appSessions.containsKey(event.packageName)) {
                                                        val sessionStartTimeMillis =
                                                                appSessions.remove(
                                                                        event.packageName
                                                                )!!
                                                        val sessionEndTimeMillis = event.timeStamp
                                                        createCardFromSession(
                                                                event.packageName,
                                                                sessionStartTimeMillis,
                                                                sessionEndTimeMillis,
                                                                processedSessionTimestamps
                                                        )
                                                        Log.d(
                                                                TAG,
                                                                "[Backfill] PAUSED/STOPPED: ${event.packageName} at ${timeFormat.format(Date(event.timeStamp))}"
                                                        )
                                                }
                                        }
                                }
                        }

                        // Handle any sessions that were still active at the end of the event log
                        // (apps still resumed)
                        appSessions.forEach { (packageName, sessionStartTimeMillis) ->
                                createCardFromSession(
                                        packageName,
                                        sessionStartTimeMillis,
                                        endOfDay,
                                        processedSessionTimestamps
                                ) // Assume session ended at end of query range
                                Log.d(
                                        TAG,
                                        "[Backfill] END_OF_LOG_SESSION: $packageName from ${timeFormat.format(Date(sessionStartTimeMillis))}"
                                )
                        }
                        Log.d(TAG, "Finished backfilling UsageCards for ${day.toLocalDate()}.")
                }
        }

        private suspend fun createCardFromSession(
                packageName: String,
                sessionStartTimeMillis: Long,
                sessionEndTimeMillis: Long,
                processedSessionTimestamps: MutableSet<Long>
        ) {
                val durationMillis = sessionEndTimeMillis - sessionStartTimeMillis
                if (durationMillis <= 60000) { // Ignore sessions shorter than 1 minute for backfill
                        Log.d(
                                TAG,
                                "[Backfill] Skipping short session for $packageName (${durationMillis / 1000}s)"
                        )
                        return
                }

                // Check if the app is in our known installed apps list
                val knownAppInfo = appInfoDao.getAppByPackageName(packageName)
                if (knownAppInfo == null || !knownAppInfo.isInstalled) {
                        Log.d(
                                TAG,
                                "[Backfill] Skipping session for $packageName as it's not a known installed app."
                        )
                        return
                }

                // Check for nearby processed sessions to avoid duplicates from rapid resume/pause
                val toleranceMillis =
                        2L * 60L * 1000L // 2 minutes tolerance window, explicitly Long
                if (processedSessionTimestamps.any {
                                kotlin.math.abs(it - sessionStartTimeMillis) < toleranceMillis
                        }
                ) {
                        Log.d(
                                TAG,
                                "[Backfill] Skipping likely duplicate session for $packageName starting near ${Date(sessionStartTimeMillis)}"
                        )
                        return
                }

                val appInfo = getAppByPackageName(packageName)
                val appLabel = appInfo?.label ?: packageName

                // Check if a similar card already exists (from OSOM's direct tracking)
                val existingCards =
                        usageCardDao
                                .getCardsForAppAroundTime(
                                        packageName,
                                        sessionStartTimeMillis,
                                        toleranceMillis
                                )
                                .firstOrNull()
                if (existingCards != null && existingCards.isNotEmpty()) {
                        Log.d(
                                TAG,
                                "[Backfill] Existing card found for $packageName around ${Date(sessionStartTimeMillis)}. Skipping backfill."
                        )
                        return
                }

                val actualDuration = Duration.ofMillis(durationMillis)
                val sessionStartDateTime =
                        LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(sessionStartTimeMillis),
                                ZoneId.systemDefault()
                        )

                val usageCard =
                        UsageCard(
                                appName = appLabel,
                                packageName = packageName,
                                openTime = sessionStartDateTime.toLocalTime(),
                                requestedDurationMinutes = null,
                                actualDuration = actualDuration,
                                title = "System-detected usage",
                                timestamp = sessionStartDateTime
                        )
                usageCardDao.insertUsageCard(usageCard)
                processedSessionTimestamps.add(sessionStartTimeMillis)
                Log.i(
                        TAG,
                        "[Backfill] Created UsageCard for $appLabel, Duration: ${actualDuration.toMinutes()} min, Start: ${sessionStartDateTime}"
                )
        }
}
