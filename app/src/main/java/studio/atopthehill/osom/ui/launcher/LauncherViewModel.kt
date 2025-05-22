package studio.atopthehill.osom.ui.launcher // Package for launcher specific UI components

// context
import android.app.Application // Android Application class for context
import android.content.Intent // Intent for launching apps
import androidx.lifecycle.AndroidViewModel // Base class for ViewModels that need Application
import androidx.lifecycle.viewModelScope // Coroutine scope tied to ViewModel lifecycle
import kotlinx.coroutines.Dispatchers // Coroutine dispatchers
import kotlinx.coroutines.flow.MutableStateFlow // Mutable state flow for UI state
import kotlinx.coroutines.flow.StateFlow // Immutable state flow for exposing UI state
import kotlinx.coroutines.flow.asStateFlow // Extension to convert mutable to immutable state flow
import kotlinx.coroutines.flow.firstOrNull // Get first element or null
import kotlinx.coroutines.launch // Coroutine builder
import studio.atopthehill.osom.OsomApplication // Custom Application class
import studio.atopthehill.osom.data.db.entity.AppInfo // AppInfo entity
import studio.atopthehill.osom.data.repository.AppRepository // AppRepository for data operations

// Enum to define the current mode of the input field
enum class InputMode {
    APP_SEARCH, // User is typing an app name to search
    AWAITING_REASON // User is being asked for a reason to open a found app
}

// Sealed class to represent different states of the app search result
sealed class ConversationState { // Renamed from SearchResult for clarity
    data object Idle : ConversationState() // Initial state, or after an action is complete
    data object Searching : ConversationState() // Search is in progress
    data class AppFound(val appInfo: AppInfo, val askReasonMessage: String) :
            ConversationState() // App found, message to ask for reason
    data object AppNotFound : ConversationState() // App not found after search and refresh
    // MultipleFound might not be needed if ViewModel always picks one, but keeping for structure
    data class MultipleAppsFound(val apps: List<AppInfo>, val confirmationMessage: String) :
            ConversationState()
    data class Error(val message: String) : ConversationState() // Error occurred
}

class LauncherViewModel(application: Application) :
        AndroidViewModel(application) { // ViewModel class

    private val appRepository: AppRepository =
            (application as OsomApplication).appRepository // Get repository from Application

    // --- UI State --- Input Text
    private val _inputText = MutableStateFlow("") // MutableStateFlow for the input text
    val inputText: StateFlow<String> = _inputText.asStateFlow() // Expose as immutable StateFlow

    // --- UI State --- Conversation State (replaces SearchResult)
    private val _conversationState = MutableStateFlow<ConversationState>(ConversationState.Idle)
    val conversationState: StateFlow<ConversationState> = _conversationState.asStateFlow()

    // --- UI State --- Current Input Mode (App Search or Awaiting Reason)
    private val _inputMode = MutableStateFlow(InputMode.APP_SEARCH)
    val inputMode: StateFlow<InputMode> = _inputMode.asStateFlow()

    // --- UI State --- App for which reason is being awaited (if any)
    private val _appAwaitingReason = MutableStateFlow<AppInfo?>(null)
    val appAwaitingReason: StateFlow<AppInfo?> =
            _appAwaitingReason.asStateFlow() // Expose as immutable StateFlow

    init { // Initialization block
        refreshAppList()
    }

    fun onInputTextChanged(newText: String) { // Function to handle input text changes
        _inputText.value = newText // Update input text state
        if (newText.isBlank() && _inputMode.value == InputMode.APP_SEARCH
        ) { // If text is blank during app search, reset state
            _conversationState.value = ConversationState.Idle
        }
    }

    // This function is called when the user presses "Search" or "Enter" on the keyboard
    fun submitInput() {
        when (_inputMode.value) {
            InputMode.APP_SEARCH -> searchForApp()
            InputMode.AWAITING_REASON -> {
                _appAwaitingReason.value?.let { launchAppWithReason(it, _inputText.value.trim()) }
            }
        }
    }

    private fun searchForApp() { // Function to initiate app search
        val query = _inputText.value.trim() // Get trimmed query
        if (query.isBlank()) { // If query is blank, do nothing or set to Idle
            _conversationState.value = ConversationState.Idle
            return
        }

        _conversationState.value = ConversationState.Searching // Set state to Searching
        viewModelScope.launch {
            val allApps =
                    appRepository.allInstalledApps.firstOrNull()
                            ?: emptyList() // Get all installed apps
            if (allApps.isEmpty()) { // If no apps, refresh and retry once
                appRepository.refreshInstalledApps() // Refresh app list
                val refreshedApps = appRepository.allInstalledApps.firstOrNull() ?: emptyList()
                performSearchLogic(query, refreshedApps) // Perform search with refreshed list
            } else {
                performSearchLogic(query, allApps) // Perform search with current list
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

        val scoredApps = mutableMapOf<AppInfo, Int>()
        appsToSearch.forEach { appInfo ->
            val appNameWords = appInfo.label.lowercase().split(' ').filter { it.isNotBlank() }
            var score = 0
            queryWords.forEach { queryWord -> if (appNameWords.contains(queryWord)) score++ }
            if (score > 0) scoredApps[appInfo] = score
        }

        if (queryWords.size > 1) {
            val concatenatedQuery = queryWords.joinToString("")
            appsToSearch.forEach { appInfo ->
                val concatenatedAppName = appInfo.label.lowercase().replace(" ", "")
                if (concatenatedAppName.contains(concatenatedQuery)) {
                    val currentScore = scoredApps.getOrDefault(appInfo, 0)
                    scoredApps[appInfo] = currentScore + queryWords.size
                }
            }
        }

        val bestScore = scoredApps.values.maxOrNull()
        if (bestScore != null && bestScore > 0) {
            val bestMatches = scoredApps.filter { it.value == bestScore }.keys.toList()
            if (bestMatches.isNotEmpty()) {
                val bestMatch = bestMatches.first() // Rule: pick first best match
                // Transition to asking for reason
                _appAwaitingReason.value = bestMatch
                _inputMode.value = InputMode.AWAITING_REASON
                _inputText.value = "" // Clear input for reason
                _conversationState.value =
                        ConversationState.AppFound(
                                bestMatch,
                                "I will open ${bestMatch.label} for you. Can you tell me the reason?"
                        )
            } else {
                tryRefreshAndSearchAgain(query) // No match, try refreshing
            }
        } else {
            tryRefreshAndSearchAgain(query) // No match, try refreshing
        }
    }

    private var hasRefreshedOnce = false // Flag to prevent infinite refresh loop

    private suspend fun tryRefreshAndSearchAgain(
            query: String
    ) { // Helper to refresh and retry search
        if (!hasRefreshedOnce) {
            hasRefreshedOnce = true
            appRepository.refreshInstalledApps()
            val refreshedApps = appRepository.allInstalledApps.firstOrNull() ?: emptyList()
            performSearchLogic(query, refreshedApps)
        } else {
            _conversationState.value = ConversationState.AppNotFound
        }
    }

    // No longer need onAppSelectedForLaunch, as performSearchLogic directly transitions state

    fun launchAppWithReason(appInfo: AppInfo, reason: String) { // Called after user provides reason
        val reasonToLog = reason.ifBlank { "Opened ${appInfo.label}" } // Use default if blank

        viewModelScope.launch {
            appRepository.logAppUsage(appInfo.packageName, reasonToLog) // Log usage
            val packageManager = getApplication<OsomApplication>().packageManager
            val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (launchIntent != null) {
                getApplication<OsomApplication>()
                        .startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                clearChatStateAfterLaunch() // Clear chat after launching
            } else {
                _conversationState.value =
                        ConversationState.Error(
                                "Could not create launch intent for ${appInfo.label}. App might be missing or disabled."
                        )
                _inputMode.value = InputMode.APP_SEARCH // Revert to app search mode on error
                _appAwaitingReason.value = null
            }
        }
    }

    fun cancelReasonPrompt() { // User might implicitly cancel by clearing text or back navigation
        clearChatStateAfterLaunch() // Treat as a full reset for now
    }

    fun refreshAppList() { // Public function to trigger app list refresh
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.refreshInstalledApps()
            hasRefreshedOnce = false
        }
    }

    // Renamed to be more specific, as clearChatState is also used for lifecycle ON_RESUME
    private fun clearChatStateAfterLaunch() {
        _inputText.value = ""
        _conversationState.value = ConversationState.Idle
        _inputMode.value = InputMode.APP_SEARCH
        _appAwaitingReason.value = null
        hasRefreshedOnce = false
    }

    // This is for lifecycle/homescreen resume
    fun resetLauncherState() {
        _inputText.value = ""
        _conversationState.value = ConversationState.Idle
        _inputMode.value = InputMode.APP_SEARCH
        _appAwaitingReason.value = null
        hasRefreshedOnce = false
    }
}
