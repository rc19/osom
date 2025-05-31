package studio.atopthehill.osom.ui.launcher // Package for launcher specific UI components

// context
import android.app.Application // Android Application class for context
import android.content.Intent // Intent for launching apps
import android.os.Build // For checking Android version
import androidx.lifecycle.AndroidViewModel // Base class for ViewModels that need Application
import androidx.lifecycle.viewModelScope // Coroutine scope tied to ViewModel lifecycle
import java.time.Duration // For UserStats
import java.time.LocalDateTime // For UserStats
import java.time.LocalTime // For UsageCard openTime
import kotlinx.coroutines.Dispatchers // Coroutine dispatchers
import kotlinx.coroutines.flow.MutableStateFlow // Mutable state flow for UI state
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow // Immutable state flow for exposing UI state
import kotlinx.coroutines.flow.asStateFlow // Extension to convert mutable to immutable state flow
import kotlinx.coroutines.flow.firstOrNull // Get first element or null
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch // Coroutine builder
import studio.atopthehill.osom.OsomApplication // Custom Application class
import studio.atopthehill.osom.data.db.entity.AppInfo // AppInfo entity
import studio.atopthehill.osom.data.db.entity.UsageCard // For creating UsageCard
import studio.atopthehill.osom.data.db.entity.UserStats // For updating UserStats
import studio.atopthehill.osom.data.repository.AppRepository // AppRepository for data operations
import studio.atopthehill.osom.services.AppTimerService // Added import for AppTimerService
import studio.atopthehill.osom.ui.navigation.Screen // For navigation

// Enum to define the current mode of the input field
enum class InputMode {
    APP_SEARCH, // User is typing an app name to search
    AWAITING_REASON, // User is being asked for a reason to open a found app
    AWAITING_DURATION // User is being asked for a duration
}

// Sealed class to represent different states of the app search result
sealed class ConversationState { // Renamed from SearchResult for clarity
    data object Idle : ConversationState() // Initial state, or after an action is complete
    data object Searching : ConversationState() // Search is in progress
    data class AppFound(val appInfo: AppInfo, val askReasonMessage: String) :
            ConversationState() // App found, message to ask for reason
    data class AskDuration(
            val appInfo: AppInfo,
            val reason: String,
            val askDurationMessage: String
    ) : ConversationState() // App and reason confirmed, asking for duration
    data object AppNotFound : ConversationState() // App not found after search and refresh
    // MultipleFound might not be needed if ViewModel always picks one, but keeping for structure
    data class MultipleAppsFound(val apps: List<AppInfo>, val confirmationMessage: String) :
            ConversationState()
    data class Error(val message: String) : ConversationState() // Error occurred
    data class LaunchingApp(val message: String) :
            ConversationState() // Added for launch confirmation
}

class LauncherViewModel(application: Application) :
        AndroidViewModel(application) { // ViewModel class

    private val appRepository: AppRepository =
            (application as OsomApplication).appRepository // Get repository from Application

    // --- Input States ---
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // --- UI State --- Conversation State
    private val _conversationState = MutableStateFlow<ConversationState>(ConversationState.Idle)
    val conversationState: StateFlow<ConversationState> = _conversationState.asStateFlow()

    // --- UI State --- Current Input Mode
    private val _inputMode = MutableStateFlow(InputMode.APP_SEARCH)
    val inputMode: StateFlow<InputMode> = _inputMode.asStateFlow()

    // --- UI State --- App for which reason/duration is being awaited
    private val _appAwaitingReasonOrDuration = MutableStateFlow<AppInfo?>(null)

    // --- UI State --- Reason collected (temporary before moving to UsageCard)
    private val _collectedReason = MutableStateFlow<String?>(null)

    // User Stats State
    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats.asStateFlow()

    private val _todaysUsageCards = MutableStateFlow<List<UsageCard>>(emptyList())
    val todaysUsageCards: StateFlow<List<UsageCard>> = _todaysUsageCards.asStateFlow()

    // --- Navigation State ---
    private val _navigateToRoute = MutableStateFlow<String?>(null)
    val navigateToRoute: StateFlow<String?> = _navigateToRoute.asStateFlow()

    // --- All Usage Cards for Summary Screen ---
    private val _allUsageCards = MutableStateFlow<List<UsageCard>>(emptyList())
    val allUsageCards: StateFlow<List<UsageCard>> = _allUsageCards.asStateFlow()

    // --- All Installed Apps for AppList Screen ---
    val allInstalledApps: StateFlow<List<AppInfo>> =
            appRepository.allInstalledApps.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList()
            )

    private var hasRefreshedOnce = false // Flag to prevent infinite refresh loop

    init { // Initialization block
        viewModelScope.launch {
            appRepository.initializeDefaultUserStatsIfNeeded()
            appRepository.getUserStats().collect { stats ->
                _userStats.value = stats
                checkDateAndResetStatsIfNeeded(stats)
            }
        }
        viewModelScope.launch {
            // Collect today's usage cards (will also be updated by backfill)
            val today = LocalDateTime.now()
            appRepository.getUsageCardsForDay(today).collect { cards ->
                _todaysUsageCards.value = cards.sortedByDescending { it.timestamp }
            }
        }
        viewModelScope.launch { // For refreshing installed apps list
            appRepository.refreshInstalledApps() // Refresh app list
            hasRefreshedOnce = true
        }
        viewModelScope.launch {
            // Attempt to backfill usage cards from system stats for today
            appRepository.backfillUsageCardsFromUsageStats(LocalDateTime.now())
        }
        resetLauncherState(showWelcome = true) // Initialize with welcome message
        loadAllUsageCards() // Load cards for summary screen initially
    }

    private fun checkDateAndResetStatsIfNeeded(currentUserStats: UserStats?) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDateTime.now().toLocalDate()
            if (currentUserStats != null &&
                            currentUserStats.lastInteraction.toLocalDate().isBefore(today)
            ) {
                val updatedStats =
                        currentUserStats.copy(
                                dailyInteractions = 0,
                                totalUsageToday = Duration.ZERO, // Reset daily stats
                                lastInteraction =
                                        LocalDateTime.now() // Update last interaction to now
                        )
                appRepository.insertOrUpdateUserStats(updatedStats)
                // _userStats.value will be updated by the collector in init
            } else if (currentUserStats == null) {
                // This case should ideally be handled by initializeDefaultUserStatsIfNeeded,
                // but as a fallback or if stats become null unexpectedly.
                appRepository.initializeDefaultUserStatsIfNeeded()
            }
        }
    }

    fun onInputTextChanged(newText: String) { // Function to handle input text changes
        _inputText.value = newText
    }

    fun submitInput() {
        val currentInput = _inputText.value.trim()

        if (currentInput.equals("summary", ignoreCase = true)) {
            _navigateToRoute.value = Screen.Summary.route
            _inputText.value = "" // Clear input
            return
        }
        if (currentInput.equals("applist", ignoreCase = true)) {
            _navigateToRoute.value = Screen.AppList.route
            _inputText.value = "" // Clear input
            return
        }

        when (_inputMode.value) {
            InputMode.APP_SEARCH -> searchForApp(currentInput)
            InputMode.AWAITING_REASON -> {
                _appAwaitingReasonOrDuration.value?.let { appInfo ->
                    processReasonInput(appInfo, currentInput)
                }
            }
            InputMode.AWAITING_DURATION -> {
                _appAwaitingReasonOrDuration.value?.let { appInfo ->
                    _collectedReason.value?.let { reason ->
                        processDurationInput(appInfo, reason, currentInput)
                    }
                }
            }
        }
    }

    private fun searchForApp(query: String) { // Function to initiate app search
        if (query.isBlank()) {
            _conversationState.value = ConversationState.Idle
            return
        }

        _conversationState.value = ConversationState.Searching
        viewModelScope.launch {
            val allApps = appRepository.allInstalledApps.firstOrNull() ?: emptyList()
            if (allApps.isEmpty() && !hasRefreshedOnce) {
                appRepository.refreshInstalledApps()
                hasRefreshedOnce = true // Mark that refresh has been attempted
                val refreshedApps = appRepository.allInstalledApps.firstOrNull() ?: emptyList()
                performSearchLogic(query, refreshedApps)
            } else {
                performSearchLogic(query, allApps)
            }
        }
    }

    private suspend fun performSearchLogic(
            query: String,
            appsToSearch: List<AppInfo>
    ) { // Private suspend function to perform search logic
        if (appsToSearch.isEmpty()) {
            _conversationState.value = ConversationState.AppNotFound
            return
        }

        val queryWords = query.lowercase().split(' ').filter { it.isNotBlank() }
        if (queryWords.isEmpty()) {
            _conversationState.value = ConversationState.Idle
            return
        }

        val matchedApps =
                appsToSearch.filter { appInfo ->
                    queryWords.all { queryWord -> appInfo.label.lowercase().contains(queryWord) }
                }

        if (matchedApps.isNotEmpty()) {
            val bestMatch = matchedApps.first()
            _appAwaitingReasonOrDuration.value = bestMatch
            _inputMode.value = InputMode.AWAITING_REASON
            _inputText.value = "" // Clear input for reason
            _conversationState.value =
                    ConversationState.AppFound(
                            bestMatch,
                            "And what do you wish to achieve by opening ${bestMatch.label}?"
                    )
        } else {
            handleAppNotFound(query)
        }
    }

    private suspend fun handleAppNotFound(query: String) {
        if (!hasRefreshedOnce) {
            hasRefreshedOnce = true
            appRepository.refreshInstalledApps()
            val refreshedApps = appRepository.allInstalledApps.firstOrNull() ?: emptyList()
            performSearchLogic(query, refreshedApps) // Retry search with refreshed list
        } else {
            _conversationState.value = ConversationState.AppNotFound
        }
    }

    private fun processReasonInput(appInfo: AppInfo, reason: String) {
        val reasonToStore = reason.ifBlank { "Opened ${appInfo.label}" } // Default reason
        _collectedReason.value = reasonToStore // Store the collected reason
        _inputMode.value = InputMode.AWAITING_DURATION
        _inputText.value = "" // Clear input for duration
        _conversationState.value =
                ConversationState.AskDuration(
                        appInfo,
                        reasonToStore,
                        "And how long would it take (in minutes)?"
                )
    }

    private fun processDurationInput(appInfo: AppInfo, reason: String, durationStr: String) {
        val durationMinutes = durationStr.toIntOrNull()
        if (durationMinutes == null || durationMinutes <= 0) {
            _conversationState.value =
                    ConversationState.Error("Please enter a valid number of minutes.")
            _inputText.value = "" // Clear invalid input
            return
        }

        var finalDurationMinutes = durationMinutes
        var launchMessage = "Opening ${appInfo.label} to \"$reason\" for $durationMinutes mins."
        if (durationMinutes > 15) {
            finalDurationMinutes = 15
            launchMessage =
                    "Opening ${appInfo.label} to \"$reason\" for $finalDurationMinutes mins (max 15 mins)."
        }

        _conversationState.value = ConversationState.LaunchingApp(launchMessage)

        viewModelScope.launch {
            // Create and store UsageCard
            val usageCard =
                    UsageCard(
                            appName = appInfo.label,
                            packageName = appInfo.packageName,
                            openTime = LocalTime.now(),
                            requestedDurationMinutes = finalDurationMinutes,
                            actualDuration = null, // Will be set on app close/timeout
                            reason = reason,
                            timestamp = LocalDateTime.now()
                    )
            val cardId = appRepository.insertUsageCard(usageCard)

            // Start the timer service
            if (cardId != -1L) { // Check if card was inserted successfully
                val serviceIntent =
                        Intent(getApplication(), AppTimerService::class.java).apply {
                            action = AppTimerService.ACTION_START_TIMER
                            putExtra(AppTimerService.EXTRA_USAGE_CARD_ID, cardId)
                        }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getApplication<OsomApplication>().startForegroundService(serviceIntent)
                } else {
                    getApplication<OsomApplication>().startService(serviceIntent)
                }
            }

            // Update UserStats
            _userStats.value?.let {
                val updatedStats =
                        it.copy(
                                dailyInteractions = it.dailyInteractions + 1,
                                lastInteraction = LocalDateTime.now()
                                // totalUsageToday will be updated by a separate mechanism or on app
                                // close
                                )
                appRepository.insertOrUpdateUserStats(updatedStats)
            }

            // Launch the app
            val packageManager = getApplication<OsomApplication>().packageManager
            val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (launchIntent != null) {
                getApplication<OsomApplication>()
                        .startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                // Don't clear chat state immediately, user sees the LaunchingApp message
                // clearChatStateAfterLaunch() // This will be handled differently, perhaps after a
                // delay or on next interaction
            } else {
                _conversationState.value =
                        ConversationState.Error(
                                "Could not launch ${appInfo.label}. App might be missing."
                        )
                // Reset relevant states if launch fails
                _inputMode.value = InputMode.APP_SEARCH
                _appAwaitingReasonOrDuration.value = null
                _collectedReason.value = null
                _inputText.value = "" // Clear input
            }
            // Reset for next interaction after a delay or explicitly by user.
            resetChatAfterAppLaunchAttempt()
        }
    }

    fun cancelInputFlow() {
        _inputText.value = ""
        _appAwaitingReasonOrDuration.value = null
        _collectedReason.value = null
        hasRefreshedOnce = false // Reset refresh flag
        // Potentially show a welcome message via resetLauncherState()
        resetLauncherState(showWelcome = true)
    }

    fun refreshAppList() { // Public function to trigger app list refresh
        viewModelScope.launch(Dispatchers.IO) {
            hasRefreshedOnce = false // Reset refresh attempt flag before new refresh
            appRepository.refreshInstalledApps()
        }
    }

    private fun resetChatAfterAppLaunchAttempt() {
        _inputText.value = ""
        _appAwaitingReasonOrDuration.value = null
        _collectedReason.value = null
        hasRefreshedOnce = false
        viewModelScope.launch {
            kotlinx.coroutines.delay(2500)
            if (_conversationState.value is ConversationState.LaunchingApp ||
                            _conversationState.value is ConversationState.Error
            ) {
                resetLauncherState(showWelcome = true)
            }
        }
    }

    fun resetLauncherState(showWelcome: Boolean = true) {
        val serviceIntent =
                Intent(getApplication(), AppTimerService::class.java).apply {
                    action = AppTimerService.ACTION_USER_RETURNED
                }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<OsomApplication>().startForegroundService(serviceIntent)
        } else {
            getApplication<OsomApplication>().startService(serviceIntent)
        }

        _inputText.value = ""
        _inputMode.value = InputMode.APP_SEARCH
        _appAwaitingReasonOrDuration.value = null
        _collectedReason.value = null
        hasRefreshedOnce = false
        // _navigateToRoute.value = null // Clear navigation trigger on reset (handled by screen
        // after nav)

        if (showWelcome) {
            val userName = _userStats.value?.userName ?: "User"
            val interactions = _userStats.value?.dailyInteractions ?: 0
            val usageDuration = _userStats.value?.totalUsageToday ?: Duration.ZERO
            val hours = usageDuration.toHours()
            val minutes = usageDuration.toMinutes() % 60
            val welcomeMessage =
                    "Welcome $userName, this is your ${interactions + 1}th interaction today. You've used your phone for $hours hrs $minutes mins. What do you wish to achieve?"
            // To ensure Idle triggers animation if already Idle, temporarily go to a different
            // state or use a different trigger
            if (_conversationState.value == ConversationState.Idle) {
                _conversationState.value =
                        ConversationState.Searching // Dummy state to force change for animation
                viewModelScope.launch {
                    kotlinx.coroutines.delay(50) // brief delay
                    _conversationState.value = ConversationState.Idle
                }
            } else {
                _conversationState.value = ConversationState.Idle
            }
            // The actual welcome message is derived by LauncherScreen from Idle state + userStats
        } else {
            _conversationState.value = ConversationState.Idle
        }
        // Refresh today's usage cards when launcher state is reset
        viewModelScope.launch {
            appRepository.getUsageCardsForDay(LocalDateTime.now()).collect { cards ->
                _todaysUsageCards.value = cards.sortedByDescending { it.timestamp }
            }
        }
    }

    fun onNavigationComplete() { // Call this from UI after navigation has occurred
        _navigateToRoute.value = null
    }

    fun loadAllUsageCards() {
        viewModelScope.launch {
            appRepository.getAllUsageCards().collect { cards
                -> // Assuming getAllUsageCards() returns Flow<List<UsageCard>>
                _allUsageCards.value = cards.sortedByDescending { it.timestamp } // Keep sorted
            }
        }
    }
}
