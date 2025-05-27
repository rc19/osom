package studio.atopthehill.osom.ui.launcher // Package for launcher specific UI components

// found
// awareness
// slideOutVertically, AnimatedVisibility, AnimatedContent
import android.app.Activity // For context casting
import android.app.Application // For ViewModel Factory
import android.content.Context // For context
import android.content.ContextWrapper // For context traversal
import androidx.compose.animation.* // Imports fadeIn, fadeOut, slideInVertically,
import androidx.compose.foundation.ExperimentalFoundationApi // Required for animateItemPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // Clickable modifier
import androidx.compose.foundation.layout.* // Layout components (Column, Row, Spacer, etc.)
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // For LazyColumn items
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
import java.time.Duration // For formatting usage duration
import kotlin.math.max
import kotlinx.coroutines.delay
import studio.atopthehill.osom.OsomApplication // Custom Application class
import studio.atopthehill.osom.data.db.entity.AppInfo // AppInfo entity
import studio.atopthehill.osom.data.db.entity.UsageCard
import studio.atopthehill.osom.ui.theme.FrauncesFontFamily // Import FrauncesFontFamily

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

        val focusManager = LocalFocusManager.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val inputFocusRequester = remember { FocusRequester() }

        LaunchedEffect(inputMode) {
                delay(200) // Keep a small delay for UI to settle before focus
                try {
                        inputFocusRequester.requestFocus()
                } catch (e: Exception) {
                        println("Focus request failed: ${e.message}")
                }
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
                                .padding(horizontal = 16.dp, vertical = 16.dp), // Overall padding
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                // Top Section: Daily Usage Timeline
                DailyUsageTimeline(cards = todaysUsageCardsFromVM)

                Spacer(modifier = Modifier.height(16.dp)) // Space between timeline and chat text

                // Middle Section: Chat Text (Centered)
                Box(
                        modifier =
                                Modifier.weight(1f) // Takes up available vertical space
                                        .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                ) {
                        // Display chat messages directly without AnimatedContent for simplicity
                        when (val currentConvState = conversationState
                        ) { // Use a stable val for when
                                is ConversationState.Idle -> {
                                        userStats?.let { stats ->
                                                val interactions = stats.dailyInteractions
                                                val interactionOrdinal = interactions + 1
                                                val usageDuration = stats.totalUsageToday
                                                val hours = usageDuration.toHours()
                                                val minutes = usageDuration.toMinutes() % 60
                                                val welcomeMsg =
                                                        "Welcome ${stats.userName ?: "User"}, this is your ${interactionOrdinal}th interaction today.You've used your phone for $hours hrs $minutes mins.What do you wish to achieve?"
                                                Text(
                                                        welcomeMsg,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        textAlign = TextAlign.Center
                                                )
                                        }
                                                ?: Text(
                                                        "Loading...",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        textAlign = TextAlign.Center
                                                )
                                }
                                is ConversationState.Searching -> {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator()
                                                Text(
                                                        "Searching...",
                                                        modifier = Modifier.padding(top = 8.dp),
                                                        style = MaterialTheme.typography.bodyLarge
                                                )
                                        }
                                }
                                is ConversationState.AppFound ->
                                        Text(
                                                currentConvState.askReasonMessage,
                                                style = MaterialTheme.typography.bodyLarge,
                                                textAlign = TextAlign.Center
                                        )
                                is ConversationState.AskDuration ->
                                        Text(
                                                currentConvState.askDurationMessage,
                                                style = MaterialTheme.typography.bodyLarge,
                                                textAlign = TextAlign.Center
                                        )
                                is ConversationState.LaunchingApp ->
                                        Text(
                                                currentConvState.message,
                                                style = MaterialTheme.typography.bodyLarge,
                                                textAlign = TextAlign.Center
                                        )
                                is ConversationState.AppNotFound ->
                                        Text(
                                                "App not found. Try a different name.",
                                                style = MaterialTheme.typography.bodyLarge,
                                                textAlign = TextAlign.Center
                                        )
                                is ConversationState.MultipleAppsFound ->
                                        Text(
                                                currentConvState.confirmationMessage,
                                                style = MaterialTheme.typography.bodyLarge,
                                                textAlign = TextAlign.Center
                                        )
                                is ConversationState.Error ->
                                        Text(
                                                "Error: ${currentConvState.message}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.error
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
                ) {
                        BasicTextField(
                                value = inputText,
                                onValueChange = { launcherViewModel.onInputTextChanged(it) },
                                modifier =
                                        Modifier.fillMaxWidth().focusRequester(inputFocusRequester),
                                textStyle =
                                        LocalTextStyle.current.copy(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontFamily = FrauncesFontFamily,
                                                textAlign = TextAlign.Center,
                                                fontSize =
                                                        MaterialTheme.typography
                                                                .titleMedium
                                                                .fontSize
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
                                decorationBox = { innerTextField ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(vertical = 12.dp),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        if (inputText.isEmpty()) {
                                                                val hintText =
                                                                        when (inputMode) {
                                                                                InputMode
                                                                                        .APP_SEARCH ->
                                                                                        "Type app name..."
                                                                                InputMode
                                                                                        .AWAITING_REASON ->
                                                                                        "Reason..."
                                                                                InputMode
                                                                                        .AWAITING_DURATION ->
                                                                                        "Duration (minutes)..."
                                                                        }
                                                                Text(
                                                                        text = hintText,
                                                                        style =
                                                                                LocalTextStyle
                                                                                        .current
                                                                                        .copy(
                                                                                                color =
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .onSurface
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.6f
                                                                                                                ),
                                                                                                fontFamily =
                                                                                                        FrauncesFontFamily,
                                                                                                textAlign =
                                                                                                        TextAlign
                                                                                                                .Center,
                                                                                                fontSize =
                                                                                                        MaterialTheme
                                                                                                                .typography
                                                                                                                .titleMedium
                                                                                                                .fontSize
                                                                                        )
                                                                )
                                                        }
                                                        innerTextField()
                                                }
                                                Divider(
                                                        color =
                                                                MaterialTheme.colorScheme.onSurface
                                                                        .copy(alpha = 0.4f),
                                                        thickness = 1.dp
                                                )
                                        }
                                }
                        )
                }
        }
}

@Composable
fun DailyUsageTimeline(cards: List<UsageCard>) {
        val totalMinutesInDayFloat = 24 * 60f

        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(20.dp)
                                .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                        MaterialTheme.shapes.small
                                )
                                .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                if (cards.isEmpty()) {
                        Spacer(
                                modifier =
                                        Modifier.weight(1f)
                                                .fillMaxHeight()
                                                .background(Color.Transparent)
                        )
                        return@Row
                }

                var accumulatedMinutes = 0f
                cards.sortedBy { it.timestamp }.forEach { card ->
                        val durationMinutes =
                                (card.actualDuration
                                                ?: Duration.ofMinutes(
                                                        card.requestedDurationMinutes.toLong()
                                                ))
                                        .toMinutes()
                                        .toFloat()
                        if (durationMinutes > 0) {
                                val segmentWeight =
                                        (durationMinutes / totalMinutesInDayFloat).coerceAtLeast(
                                                0.001f
                                        )
                                Box(
                                        modifier =
                                                Modifier.fillMaxHeight()
                                                        .weight(segmentWeight)
                                                        .background(
                                                                MaterialTheme.colorScheme.primary
                                                                        .copy(alpha = 0.6f),
                                                                MaterialTheme.shapes.small
                                                        )
                                )
                                accumulatedMinutes += durationMinutes
                                if (cards.last() != card) {
                                        Spacer(
                                                Modifier.width(1.dp)
                                                        .fillMaxHeight()
                                                        .background(
                                                                MaterialTheme.colorScheme.background
                                                                        .copy(alpha = 0.5f)
                                                        )
                                        )
                                }
                        }
                }

                val remainingMinutes = max(0f, totalMinutesInDayFloat - accumulatedMinutes)
                if (remainingMinutes > 0f) {
                        Spacer(
                                modifier =
                                        Modifier.weight(
                                                        (remainingMinutes / totalMinutesInDayFloat)
                                                                .coerceAtLeast(0.001f)
                                                )
                                                .fillMaxHeight()
                                                .background(Color.Transparent)
                        )
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
                                        style = MaterialTheme.typography.titleMedium
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
                title = { Text("Error", fontFamily = FrauncesFontFamily) },
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
                                        style =
                                                MaterialTheme.typography.titleMedium.copy(
                                                        fontFamily = FrauncesFontFamily
                                                )
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
