package studio.atopthehill.osom.ui.launcher

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import studio.atopthehill.osom.OsomApplication
import studio.atopthehill.osom.data.db.entity.AppInfo
import studio.atopthehill.osom.data.db.entity.UsageCard
import studio.atopthehill.osom.data.db.entity.UserStats
import studio.atopthehill.osom.ui.navigation.Screen
import studio.atopthehill.osom.ui.theme.EInkAccent
import studio.atopthehill.osom.ui.theme.EInkBackground
import studio.atopthehill.osom.ui.theme.EInkLineArt
import studio.atopthehill.osom.ui.theme.EInkTextPrimary
import studio.atopthehill.osom.ui.theme.FrauncesFontFamily
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
    onOnboardingComplete: () -> Unit,
    launcherViewModel: LauncherViewModel = viewModel(
        factory = LauncherViewModelFactory(
            LocalContext.current.applicationContext as OsomApplication
        )
    )
) {
    val showOnboarding by launcherViewModel.showOnboarding.collectAsStateWithLifecycle()
    val navigateToRoute by launcherViewModel.navigateToRoute.collectAsStateWithLifecycle()

    LaunchedEffect(navigateToRoute) {
        navigateToRoute?.let { route ->
            navController.navigate(route)
            launcherViewModel.onNavigationComplete()
        }
    }

    if (showOnboarding) {
        OnboardingCarousel(
            onOnboardingComplete = onOnboardingComplete
        )
    } else {
        MainLauncherContent(navController, launcherViewModel)
    }
}

data class OnboardingPageData(
    val imageVector: ImageVector,
    val title: String,
    val body: String,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingCarousel(onOnboardingComplete: () -> Unit) {
    val pages =
        listOf(
            OnboardingPageData(
                imageVector = Icons.Default.Favorite,
                title = "Remember Everything. 🧠",
                body =
                "Osom is your second brain, proactively finding tasks and commitments so you don’t have to.",
            ),
            OnboardingPageData(
                imageVector = Icons.Default.Lock,
                title = "Intelligent & Private. 🤫",
                body =
                "Osom reads your screen to identify tasks. All processing happens on your device. Your data never leaves your phone.",
            ),
            OnboardingPageData(
                imageVector = Icons.Default.CheckCircle,
                title = "Ready to get started? ✅",
                body = "",
            )
        )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(
            onClick = onOnboardingComplete,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Skip")
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { pageIndex ->
            OnboardingPage(page = pages[pageIndex])
        }

        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color =
                    if (pagerState.currentPage == iteration) Color.Black
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }

        if (pagerState.currentPage == pages.lastIndex) {
            Button(
                onClick = onOnboardingComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                Text("Let's Go 🚀")
            }
        } else {
            // Keep the space for the button to avoid layout jumps
            Spacer(modifier = Modifier.height(72.dp)) // Approximate height of the button + padding
        }
    }
}

@Composable
fun OnboardingPage(page: OnboardingPageData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = page.imageVector,
            contentDescription = page.title,
            modifier = Modifier.size(128.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = page.title,
            fontFamily = FrauncesFontFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = Color(0xFF333333)
        )
        if (page.body.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = page.body,
                fontFamily = PromptFontFamily,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun MainLauncherContent(navController: NavController, launcherViewModel: LauncherViewModel) {
    val inputText by launcherViewModel.inputText.collectAsStateWithLifecycle()
    val conversationState by launcherViewModel.conversationState.collectAsStateWithLifecycle()
    val inputMode by launcherViewModel.inputMode.collectAsStateWithLifecycle()
    val userStats by launcherViewModel.userStats.collectAsStateWithLifecycle()
    val todaysUsageCardsFromVM by
    launcherViewModel.todaysUsageCards.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val inputFocusRequester = remember { FocusRequester() }

    // Determine screen content using the helper function
    val screenContent = determineScreenContent(conversationState, inputMode, userStats)

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
        Modifier
            .fillMaxSize()
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
            Modifier
                .weight(1f) // Takes up available vertical space
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
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp
                ) // Padding for the input area itself
                .imePadding() // Added IME padding
        ) {
            BasicTextField(
                value = inputText,
                onValueChange = { launcherViewModel.onInputTextChanged(it) },
                modifier =
                Modifier
                    .fillMaxWidth()
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
        Modifier
            .fillMaxWidth()
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
                    Modifier
                        .weight(1f)
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
                    Duration
                        .between(periodStartDateTime, cardStartDateTime)
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
                        Modifier
                            .fillMaxHeight()
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
                        Modifier
                            .fillMaxHeight()
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
                    Modifier
                        .weight(remainingWeight)
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
                            Modifier
                                .fillMaxWidth()
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
        title = { Text("Error") },
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
                            Modifier
                                .fillMaxWidth()
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

@Composable
fun determineScreenContent(
    conversationState: ConversationState,
    inputMode: InputMode,
    userStats: UserStats?
): ScreenContent {
    val userName = userStats?.userName ?: "User"
    val interactions = userStats?.dailyInteractions ?: 0
    val usageDuration = userStats?.totalUsageToday ?: Duration.ZERO
    val hours = usageDuration.toHours()
    val minutes = usageDuration.toMinutes() % 60

    val welcomeMessage =
        "Welcome $userName, this is your ${interactions + 1}th interaction today. You've used your phone for $hours hrs $minutes mins. What do you wish to achieve?"

    return when (conversationState) {
        is ConversationState.Idle -> ScreenContent(welcomeMessage, "Search for an app or type a command...")
        is ConversationState.Searching -> ScreenContent("Searching...", "")
        is ConversationState.AppFound -> ScreenContent(
            conversationState.askReasonMessage,
            "Why are you opening this app?"
        )
        is ConversationState.AskDuration -> ScreenContent(
            conversationState.askDurationMessage,
            "How long do you need (in minutes)?"
        )
        is ConversationState.AppNotFound -> ScreenContent(
            "Sorry, I couldn't find that app.",
            "Try searching again."
        )
        is ConversationState.MultipleAppsFound -> ScreenContent(
            conversationState.confirmationMessage,
            "Please select one."
        )
        is ConversationState.Error -> ScreenContent(
            "Error: ${conversationState.message}",
            "Please try again."
        )
        is ConversationState.LaunchingApp -> ScreenContent(conversationState.message, "")
    }
}

data class ScreenContent(val centerText: String, val inputHintText: String)
