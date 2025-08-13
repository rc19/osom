package studio.atopthehill.osom.ui.launcher

import studio.atopthehill.osom.data.db.entity.UserStats



// Helper function for ordinal numbers (copied from LauncherScreen.kt)
private fun getOrdinalSuffix(n: Int): String {
        if (n % 100 in 11..13) {
                return "th"
        }
        return when (n % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
        }
}

fun determineScreenContent(
        conversationState: ConversationState,
        inputMode: InputMode,
        userStats: UserStats?,
        userNamePlaceholder: String = "User" // Default if userStats or userName is null
): ScreenContent {

        val centerText =
                when (conversationState) {
                        is ConversationState.Idle -> {
                                userStats?.let { stats ->
                                        val interactions = stats.dailyInteractions
                                        val interactionOrdinal = interactions + 1
                                        val suffix = getOrdinalSuffix(interactionOrdinal)
                                        val usageDuration = stats.totalUsageToday
                                        val hours = usageDuration.toHours()
                                        val minutes = usageDuration.toMinutes() % 60
                                        "Welcome ${stats.userName ?: userNamePlaceholder}, this is your $interactionOrdinal$suffix interaction today.\nYou've used your phone for $hours hrs $minutes mins.\nWhat do you wish to achieve?"
                                }
                                        ?: "Loading..."
                        }
                        is ConversationState.Searching ->
                                "Searching..." // Progress indicator will be shown separately
                        is ConversationState.AppFound -> conversationState.askReasonMessage
                        is ConversationState.AskDuration -> conversationState.askDurationMessage
                        is ConversationState.LaunchingApp -> conversationState.message
                        is ConversationState.AppNotFound -> "App not found. Try a different name."
                        is ConversationState.MultipleAppsFound ->
                                conversationState.confirmationMessage
                        is ConversationState.Error -> "Error: ${conversationState.message}"
                }

        val inputHintText =
                when (inputMode) {
                        InputMode.APP_SEARCH -> "Type app name..."
                        InputMode.AWAITING_REASON -> "Reason..."
                        InputMode.AWAITING_DURATION -> "Duration (minutes)..."
                }

        return ScreenContent(centerText, inputHintText)
}
