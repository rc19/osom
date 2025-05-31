package studio.atopthehill.osom.ui.launcher // Package for launcher specific UI components

// found
// awareness
// slideOutVertically, AnimatedVisibility, AnimatedContent
// missing)
import android.app.Activity // For context casting
import android.app.Application // For ViewModel Factory
import android.content.Context // For context
import android.content.ContextWrapper // For context traversal
import androidx.compose.animation.* // Imports fadeIn, fadeOut, slideInVertically,
import androidx.compose.foundation.BorderStroke // Added for border
import androidx.compose.foundation.ExperimentalFoundationApi // Required for animateItemPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border // Added for border modifier
import androidx.compose.foundation.clickable // Clickable modifier
import androidx.compose.foundation.layout.* // Layout components (Column, Row, Spacer, etc.)
import androidx.compose.foundation.layout.imePadding // Added for TextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // For LazyColumn items
import androidx.compose.foundation.shape.CircleShape // Added for TextField border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions // Keyboard actions for TextField
import androidx.compose.foundation.text.KeyboardOptions // Keyboard options for TextField
import androidx.compose.material3.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface // For the input card background
import androidx.compose.runtime.* // Composable, remember, State, etc.
import androidx.compose.ui.Alignment // Alignment modifiers
import androidx.compose.ui.Modifier // Modifier for UI elements
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow // Added for TextField shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color // Import for Color.Transparent
import androidx.compose.ui.graphics.SolidColor // Added import for SolidColor
import androidx.compose.ui.platform.LocalContext // To get current context
import androidx.compose.ui.platform.LocalFocusManager // To manage focus
import androidx.compose.ui.platform.LocalLifecycleOwner // To get lifecycle owner
import androidx.compose.ui.text.input.ImeAction // IME action for TextField (e.g., Search or Done)
import androidx.compose.ui.text.input.KeyboardCapitalization // Keyboard capitalization
import androidx.compose.ui.text.input.KeyboardType // Keyboard type (e.g., Text)
import androidx.compose.ui.text.style.TextAlign // Added import for TextAlign
import androidx.compose.ui.unit.dp // Density-independent pixels for spacing and sizing
import androidx.compose.ui.window.Dialog // For reason prompt dialog
import androidx.lifecycle.Lifecycle // For lifecycle events
import androidx.lifecycle.ViewModelProvider // For ViewModel Factory
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Collect StateFlow with lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel // To get ViewModel instance
import androidx.navigation.NavController
import java.time.Duration // For formatting usage duration
import java.time.LocalDateTime // Added for DailyUsageTimeline
import kotlinx.coroutines.delay
import studio.atopthehill.osom.OsomApplication // Custom Application class
import studio.atopthehill.osom.data.db.entity.AppInfo // AppInfo entity
import studio.atopthehill.osom.data.db.entity.UsageCard
import studio.atopthehill.osom.data.db.entity.UserStats // Added for DailyUsageTimeline
import studio.atopthehill.osom.ui.theme.EInkAccent // Added for DailyUsageTimeline
import studio.atopthehill.osom.ui.theme.EInkBackground // Added for TextField
import studio.atopthehill.osom.ui.theme.EInkLineArt // For TextField border
import studio.atopthehill.osom.ui.theme.EInkTextPrimary // Added for DailyUsageTimeline
import studio.atopthehill.osom.ui.theme.PromptFontFamily

// Helper function to get Activity from Context, useful for things like ViewModelStoreOwner
fun Context.getActivity(): Activity? =
        when (this) {
                is Activity -> this
                is ContextWrapper -> baseContext.getActivity()
                else -> null
        }

// ViewModel Factory to pass Application to AndroidViewModel
class LauncherViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LauncherViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST") return LauncherViewModel(application) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
}

@OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class
)
@Composable
fun LauncherScreen(
        navController: NavController,
        launcherViewModel: LauncherViewModel =
                viewModel(
                        factory =
                                LauncherViewModelFactory(
                                        LocalContext.current.applicationContext as OsomApplication
                                )
                )
) {
        val inputText by launcherViewModel.inputText.collectAsStateWithLifecycle()
        val conversationState by launcherViewModel.conversationState.collectAsStateWithLifecycle()
        val inputMode by launcherViewModel.inputMode.collectAsStateWithLifecycle()
        val userStats by launcherViewModel.userStats.collectAsStateWithLifecycle()
        val todaysUsageCardsFromVM by
                launcherViewModel.todaysUsageCards.collectAsStateWithLifecycle()
        val navigateToRoute by launcherViewModel.navigateToRoute.collectAsStateWithLifecycle()

        val focusManager = LocalFocusManager.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val inputFocusRequester = remember { FocusRequester() }

        // Determine screen content using the helper function
        val screenContent = determineScreenContent(conversationState, inputMode, userStats)

        LaunchedEffect(navigateToRoute) {
                navigateToRoute?.let { route ->
                        navController.navigate(route)
                        launcherViewModel.onNavigationComplete()
                }
        }

        LaunchedEffect(inputMode) {
                if (inputMode == InputMode.AWAITING_REASON ||
                                inputMode == InputMode.AWAITING_DURATION
                ) {
                        delay(200) // Keep a small delay for UI to settle before focus
                        try {
                                inputFocusRequester.requestFocus()
                        } catch (e: Exception) {
                                println("Focus request failed: ${e.message}")
                        }
                }
                // Removed explicit focus request for InputMode.APP_SEARCH
                // User tap will naturally focus the field in this mode.
        }

        DisposableEffect(lifecycleOwner) {
                val observer =
                        androidx.lifecycle.LifecycleEventObserver { _, event ->
                                if (event == Lifecycle.Event.ON_RESUME) {
                                        launcherViewModel.resetLauncherState(showWelcome = true)
                                }
                        }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(
                                        start = 16.dp,
                                        top = 40.dp,
                                        end = 16.dp,
                                        bottom = 40.dp
                                ), // Overall padding
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                // Top Section: Daily Usage Timeline
                DailyUsageTimeline(userStats, todaysUsageCardsFromVM)

                Spacer(modifier = Modifier.height(16.dp)) // Space between timeline and chat text

                // Middle Section: Chat Text (Centered)
                Box(
                        modifier =
                                Modifier.weight(1f) // Takes up available vertical space
                                        .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                ) {
                        // Display chat messages directly without AnimatedContent for simplicity
                        if (conversationState is ConversationState.Searching) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator()
                                        Text(
                                                screenContent.centerText,
                                                modifier = Modifier.padding(top = 8.dp),
                                                style = MaterialTheme.typography.headlineMedium
                                        )
                                }
                        } else {
                                Text(
                                        screenContent.centerText,
                                        style = MaterialTheme.typography.headlineMedium,
                                        textAlign = TextAlign.Center,
                                        color =
                                                if (conversationState is ConversationState.Error)
                                                        MaterialTheme.colorScheme.error
                                                else LocalContentColor.current
                                )
                        }
                }

                Spacer(modifier = Modifier.height(16.dp)) // Space between chat text and input field

                // Bottom Section: Input Field
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(
                                                horizontal = 8.dp
                                        ) // Padding for the input area itself
                                        .imePadding() // Added IME padding
                ) {
                        BasicTextField(
                                value = inputText,
                                onValueChange = { launcherViewModel.onInputTextChanged(it) },
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .shadow(elevation = 2.dp, shape = CircleShape)
                                                .border(
                                                        BorderStroke(1.dp, EInkLineArt),
                                                        CircleShape
                                                )
                                                .background(EInkBackground, CircleShape)
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                                .focusRequester(inputFocusRequester),
                                textStyle =
                                        LocalTextStyle.current.copy(
                                                color =
                                                        MaterialTheme.colorScheme
                                                                .onSurface, // Will be
                                                // EInkTextPrimary from
                                                // theme
                                                fontFamily = PromptFontFamily,
                                                textAlign = TextAlign.Center,
                                                fontSize =
                                                        MaterialTheme.typography
                                                                .bodyLarge
                                                                .fontSize // Changed to bodyLarge
                                        ),
                                keyboardOptions =
                                        KeyboardOptions.Default.copy(
                                                imeAction =
                                                        when (inputMode) {
                                                                InputMode.APP_SEARCH ->
                                                                        ImeAction.Search
                                                                else -> ImeAction.Done
                                                        },
                                                keyboardType =
                                                        when (inputMode) {
                                                                InputMode.AWAITING_DURATION ->
                                                                        KeyboardType.Number
                                                                else -> KeyboardType.Text
                                                        },
                                                capitalization =
                                                        when (inputMode) {
                                                                InputMode.AWAITING_REASON ->
                                                                        KeyboardCapitalization
                                                                                .Sentences
                                                                else -> KeyboardCapitalization.None
                                                        }
                                        ),
                                keyboardActions =
                                        KeyboardActions(
                                                onSearch = {
                                                        if (inputMode == InputMode.APP_SEARCH)
                                                                launcherViewModel.submitInput()
                                                },
                                                onDone = {
                                                        if (inputMode != InputMode.APP_SEARCH)
                                                                launcherViewModel.submitInput()
                                                }
                                        ),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField
                                        -> // Simplified decorationBox for hint
                                        Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                if (inputText.isEmpty()) {
                                                        Text(
                                                                text = screenContent.inputHintText,
                                                                style =
                                                                        LocalTextStyle.current.copy(
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSurface
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.6f
                                                                                                ),
                                                                                fontFamily =
                                                                                        PromptFontFamily,
                                                                                textAlign =
                                                                                        TextAlign
                                                                                                .Center,
                                                                                fontSize =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyLarge
                                                                                                .fontSize // Consistent hint size
                                                                        )
                                                        )
                                                }
                                                innerTextField()
                                        }
                                }
                        )
                }
        }
}

@Composable
fun DailyUsageTimeline(userStats: UserStats?, cards: List<UsageCard>) {
        val dayStartHour = userStats?.dayStartTimeHour ?: 3
        val now = LocalDateTime.now()

        // Determine the current day's period based on dayStartHour
        val todayStartDateTime = now.toLocalDate().atTime(dayStartHour, 0)
        val periodStartDateTime =
                if (now.hour < dayStartHour) {
                        todayStartDateTime.minusDays(1)
                } else {
                        todayStartDateTime
                }
        val periodEndDateTime = periodStartDateTime.plusHours(24)

        val totalMinutesInPeriod = 24 * 60f

        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(24.dp) // Increased height for better visibility and border
                                .border(
                                        BorderStroke(1.dp, EInkTextPrimary),
                                        MaterialTheme.shapes.small
                                )
                                .padding(2.dp) // Padding inside the border
        ) {
                Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        if (cards.isEmpty()) {
                                Spacer(
                                        modifier =
                                                Modifier.weight(1f)
                                                        .fillMaxHeight()
                                                        .background(
                                                                Color.Transparent
                                                        ) // Background of the empty bar area
                                )
                                return@Row
                        }

                        val relevantCards =
                                cards
                                        .filter {
                                                val cardDateTime = it.timestamp
                                                !cardDateTime.isBefore(periodStartDateTime) &&
                                                        cardDateTime.isBefore(periodEndDateTime)
                                        }
                                        .sortedBy { it.timestamp }

                        var currentMinuteMarker = 0f

                        relevantCards.forEach { card ->
                                val cardStartDateTime = card.timestamp
                                val cardStartOffsetMinutes =
                                        Duration.between(periodStartDateTime, cardStartDateTime)
                                                .toMinutes()
                                                .toFloat()

                                val durationToDraw =
                                        card.actualDuration
                                                ?: card.requestedDurationMinutes
                                                        ?.takeIf { it > 0 }
                                                        ?.let { Duration.ofMinutes(it.toLong()) }

                                val durationMinutes = durationToDraw?.toMinutes()?.toFloat() ?: 0f

                                // Space before this card's segment (if any)
                                val spaceBeforeMinutes =
                                        (cardStartOffsetMinutes - currentMinuteMarker)
                                                .coerceAtLeast(0f)
                                if (spaceBeforeMinutes > 0) {
                                        val spaceWeight =
                                                (spaceBeforeMinutes / totalMinutesInPeriod)
                                                        .coerceAtLeast(0.0001f)
                                        Spacer(
                                                modifier =
                                                        Modifier.fillMaxHeight()
                                                                .weight(spaceWeight)
                                                                .background(Color.Transparent)
                                        )
                                }

                                // This card's segment
                                if (durationMinutes > 0) {
                                        val segmentWeight =
                                                (durationMinutes / totalMinutesInPeriod)
                                                        .coerceAtLeast(0.001f)
                                        Box(
                                                modifier =
                                                        Modifier.fillMaxHeight()
                                                                .weight(segmentWeight)
                                                                .background(
                                                                        EInkAccent,
                                                                        MaterialTheme.shapes.small
                                                                )
                                        )
                                        currentMinuteMarker =
                                                cardStartOffsetMinutes + durationMinutes
                                } else {
                                        currentMinuteMarker =
                                                cardStartOffsetMinutes // No duration, just move
                                        // marker
                                }
                        }

                        // Remaining space at the end of the timeline
                        val remainingMinutes =
                                (totalMinutesInPeriod - currentMinuteMarker).coerceAtLeast(0f)
                        if (remainingMinutes > 0f) {
                                val remainingWeight =
                                        (remainingMinutes / totalMinutesInPeriod).coerceAtLeast(
                                                0.0001f
                                        )
                                Spacer(
                                        modifier =
                                                Modifier.weight(remainingWeight)
                                                        .fillMaxHeight()
                                                        .background(Color.Transparent)
                                )
                        }
                }
        }
}

@Composable
fun ReasonPromptDialog(appInfo: AppInfo, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
        var reasonText by remember { mutableStateOf("") }
        val focusManager = LocalFocusManager.current

        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Launch ${appInfo.label}?") },
                text = {
                        Column {
                                Text("Why do you want to open this app?")
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                        value = reasonText,
                                        onValueChange = { reasonText = it },
                                        label = { Text("Reason (optional)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions =
                                                KeyboardOptions.Default.copy(
                                                        capitalization =
                                                                KeyboardCapitalization.Sentences,
                                                        imeAction = ImeAction.Done
                                                ),
                                        keyboardActions =
                                                KeyboardActions(
                                                        onDone = {
                                                                onConfirm(
                                                                        reasonText.ifBlank {
                                                                                "Opened ${appInfo.label}"
                                                                        }
                                                                )
                                                                focusManager.clearFocus()
                                                        }
                                                )
                                )
                        }
                },
                confirmButton = {
                        Button(
                                onClick = {
                                        onConfirm(reasonText.ifBlank { "Opened ${appInfo.label}" })
                                        focusManager.clearFocus()
                                },
                        ) { Text("Open App") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
}

@Composable
fun MultipleAppsFoundDialog(
        apps: List<AppInfo>,
        onAppSelected: (AppInfo) -> Unit,
        onDismiss: () -> Unit
) {
        Dialog(onDismissRequest = onDismiss) {
                Surface(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 8.dp
                ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                        "Multiple apps found:",
                                        style = MaterialTheme.typography.headlineMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(apps) { app ->
                                                Text(
                                                        text = app.label,
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .clickable {
                                                                                onAppSelected(app)
                                                                        }
                                                                        .padding(vertical = 8.dp)
                                                )
                                        }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                ) { TextButton(onClick = onDismiss) { Text("Cancel") } }
                        }
                }
        }
}

@Composable
fun ErrorDialog(message: String, onDismiss: () -> Unit) {
        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Error", fontFamily = PromptFontFamily) },
                text = { Text(message) },
                confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
        )
}

@Composable
fun ConfirmAppSelectionDialog(
        apps: List<AppInfo>,
        onAppSelected: (AppInfo) -> Unit,
        onDismiss: () -> Unit
) {
        Dialog(onDismissRequest = onDismiss) {
                Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                        "Did you mean:",
                                        style = MaterialTheme.typography.headlineMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                LazyColumn {
                                        items(apps) { app ->
                                                Text(
                                                        text = app.label,
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .clickable {
                                                                                onAppSelected(app)
                                                                        }
                                                                        .padding(vertical = 8.dp)
                                                )
                                        }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                        onClick = onDismiss,
                                        modifier = Modifier.align(Alignment.End)
                                ) { Text("Cancel") }
                        }
                }
        }
}

fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        return when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                else -> "${minutes}m"
        }
}
