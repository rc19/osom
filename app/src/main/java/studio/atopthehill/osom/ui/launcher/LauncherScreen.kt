package studio.atopthehill.osom.ui.launcher // Package for launcher specific UI components

// found
// awareness
// slideOutVertically, AnimatedVisibility, AnimatedContent
import android.app.Activity // For context casting
import android.app.Application // For ViewModel Factory
import android.content.Context // For context
import android.content.ContextWrapper // For context traversal
import androidx.compose.animation.* // Imports fadeIn, fadeOut, slideInVertically,
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi // Required for animateItemPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // Clickable modifier
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.* // Layout components (Column, Row, Spacer, etc.)
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // For LazyColumn items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext // To get current context
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager // To manage focus
import androidx.compose.ui.platform.LocalLifecycleOwner // To get lifecycle owner
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction // IME action for TextField (e.g., Search or Done)
import androidx.compose.ui.text.input.KeyboardCapitalization // Keyboard capitalization
import androidx.compose.ui.text.input.KeyboardType // Keyboard type (e.g., Text)
import androidx.compose.ui.text.style.TextAlign // Added import for TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp // Density-independent pixels for spacing and sizing
import androidx.compose.ui.window.Dialog // For reason prompt dialog
import androidx.lifecycle.Lifecycle // For lifecycle events
import androidx.lifecycle.ViewModelProvider // For ViewModel Factory
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Collect StateFlow with lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel // To get ViewModel instance
import java.time.Duration // For formatting usage duration
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

        val displayedUsageCards = remember { mutableStateListOf<UsageCard>() }
        val lazyListState = rememberLazyListState()

        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        // Let bottom area (chat + input) take about 30-35% of screen height initially
        // This means cards' bottom edge will be at this mark from screen bottom.
        val bottomAreaHeight = screenHeight * 0.33f

        val isListScrolled by remember {
                derivedStateOf {
                        // The list is considered "scrolled" if it's not at the very beginning.
                        // (i.e., the bottom input area should only be visible if absolutely no
                        // scroll has occurred)
                        lazyListState.firstVisibleItemIndex > 0 ||
                                lazyListState.firstVisibleItemScrollOffset > 0
                }
        }

        LaunchedEffect(todaysUsageCardsFromVM) {
                val currentIds = displayedUsageCards.map { it.id }.toSet()
                val newCards = todaysUsageCardsFromVM.filterNot { currentIds.contains(it.id) }
                // Add new cards. With reverseLayout=true, they appear at the "bottom" of the list
                // data,
                // which is the visual start (bottom of screen).
                displayedUsageCards.addAll(0, newCards.sortedByDescending { it.timestamp })

                val vmIds = todaysUsageCardsFromVM.map { it.id }.toSet()
                displayedUsageCards.retainAll { vmIds.contains(it.id) }

                // Ensure consistent sort order in displayedUsageCards, newest first (for
                // reverseLayout)
                if (displayedUsageCards.isNotEmpty()) {
                        displayedUsageCards.sortWith(compareByDescending { it.timestamp })
                }
        }

        val focusManager = LocalFocusManager.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val inputFocusRequester = remember { FocusRequester() }

        LaunchedEffect(inputMode, isListScrolled) {
                if (!isListScrolled) {
                        delay(250) // Slightly longer delay for focus request stability
                        try {
                                inputFocusRequester.requestFocus()
                        } catch (e: Exception) {
                                println("Focus request failed: ${e.message}")
                        }
                } else {
                        if (inputFocusRequester.captureFocus()) {
                                focusManager.clearFocus()
                        }
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

        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) { // Main container
                LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                                PaddingValues(
                                        top = 16.dp, // General top padding for the list
                                        bottom =
                                                if (!isListScrolled)
                                                        bottomAreaHeight +
                                                                16.dp // Space for bottom area +
                                                // extra padding
                                                else 16.dp // Default bottom padding when scrolled
                                ),
                        reverseLayout =
                                true // Newest items (UsageCards) at the visual bottom, appearing
                        // from bottom
                        ) {
                        // Sticky Header: DailyUsageTimeline
                        // With reverseLayout=true, this will appear at the visual top of the card
                        // group
                        // and stick there as cards scroll "under" it.
                        stickyHeader {
                                Column(
                                        Modifier.background(
                                                MaterialTheme.colorScheme.background.copy(
                                                        alpha = 0.9f
                                                )
                                        )
                                ) { // Opaque BG
                                        DailyUsageTimeline(
                                                cards = todaysUsageCardsFromVM
                                        ) // Use VM's list for accuracy
                                        Spacer(
                                                modifier = Modifier.height(8.dp)
                                        ) // Spacing after timeline before cards list
                                }
                        }

                        // Hall of Shame Cards
                        items(
                                items = displayedUsageCards,
                                key = { card -> "usage_card_${card.id}" } // Ensure unique keys
                        ) { card ->
                                SwipeableUsageCardItem(
                                        card = card,
                                        onDismissed = {
                                                // launcherViewModel.onUsageCardDismissed(card.id)
                                                // // TODO: Implement in ViewModel for persistence
                                                displayedUsageCards.remove(
                                                        card
                                                ) // Optimistic local removal
                                        },
                                        modifier =
                                                Modifier.animateItemPlacement(
                                                                animationSpec =
                                                                        tween(durationMillis = 350)
                                                        )
                                                        .padding(
                                                                vertical = 4.dp
                                                        ) // Padding between cards
                                )
                        }
                }

                // Bottom Area: Chat Text + Input Field
                AnimatedVisibility(
                        visible = !isListScrolled,
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                        enter =
                                fadeIn(animationSpec = tween(200)) +
                                        slideInVertically(
                                                animationSpec = tween(250),
                                                initialOffsetY = { it / 2 }
                                        ),
                        exit =
                                fadeOut(animationSpec = tween(200)) +
                                        slideOutVertically(
                                                animationSpec = tween(250),
                                                targetOffsetY = { it / 2 }
                                        )
                ) {
                        Column(
                                modifier =
                                        Modifier.height(
                                                        bottomAreaHeight
                                                ) // Defined height for this entire bottom section
                                                .fillMaxWidth()
                                                // .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) // Optional: for debugging layout
                                                .padding(
                                                        bottom = 8.dp
                                                ), // Padding for the whole bottom group from screen
                                // edge (e.g., nav bar)
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                // Central Chat Text Area
                                Box(
                                        modifier =
                                                Modifier.weight(
                                                                1f
                                                        ) // Takes up available vertical space above
                                                        // input field
                                                        .fillMaxWidth()
                                                        .padding(
                                                                vertical = 8.dp
                                                        ), // Padding around chat text
                                        contentAlignment = Alignment.Center
                                ) {
                                        AnimatedContent(
                                                targetState = conversationState,
                                                transitionSpec = {
                                                        (slideInHorizontally { width ->
                                                                        width / 2
                                                                } +
                                                                        fadeIn(
                                                                                animationSpec =
                                                                                        tween(
                                                                                                durationMillis =
                                                                                                        300,
                                                                                                delayMillis =
                                                                                                        150
                                                                                        )
                                                                        ))
                                                                .togetherWith(
                                                                        slideOutHorizontally { width
                                                                                ->
                                                                                -width / 2
                                                                        } +
                                                                                fadeOut(
                                                                                        animationSpec =
                                                                                                tween(
                                                                                                        durationMillis =
                                                                                                                150
                                                                                                )
                                                                                )
                                                                )
                                                },
                                                contentAlignment = Alignment.Center,
                                                label = "ConversationStateAnimationInBottomArea"
                                        ) { targetState ->
                                                Box(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        when (targetState) {
                                                                is ConversationState.Idle -> {
                                                                        userStats?.let { stats ->
                                                                                val interactions =
                                                                                        stats.dailyInteractions
                                                                                val interactionOrdinal =
                                                                                        interactions +
                                                                                                1
                                                                                val usageDuration =
                                                                                        stats.totalUsageToday
                                                                                val hours =
                                                                                        usageDuration
                                                                                                .toHours()
                                                                                val minutes =
                                                                                        usageDuration
                                                                                                .toMinutes() %
                                                                                                60
                                                                                val welcomeMsg =
                                                                                        "Welcome ${stats.userName ?: "User"}, this is your ${interactionOrdinal}th interaction today.You've used your phone for $hours hrs $minutes mins.What do you wish to achieve?"
                                                                                Text(
                                                                                        welcomeMsg,
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .bodyLarge,
                                                                                        textAlign =
                                                                                                TextAlign
                                                                                                        .Center
                                                                                )
                                                                        }
                                                                                ?: Text(
                                                                                        "Loading...",
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .bodyLarge,
                                                                                        textAlign =
                                                                                                TextAlign
                                                                                                        .Center
                                                                                )
                                                                }
                                                                is ConversationState.Searching -> {
                                                                        Column(
                                                                                horizontalAlignment =
                                                                                        Alignment
                                                                                                .CenterHorizontally
                                                                        ) {
                                                                                CircularProgressIndicator()
                                                                                Text(
                                                                                        "Searching...",
                                                                                        modifier =
                                                                                                Modifier.padding(
                                                                                                        top =
                                                                                                                8.dp
                                                                                                ),
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .bodyLarge
                                                                                )
                                                                        }
                                                                }
                                                                is ConversationState.AppFound ->
                                                                        Text(
                                                                                targetState
                                                                                        .askReasonMessage,
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyLarge,
                                                                                textAlign =
                                                                                        TextAlign
                                                                                                .Center
                                                                        )
                                                                is ConversationState.AskDuration ->
                                                                        Text(
                                                                                targetState
                                                                                        .askDurationMessage,
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyLarge,
                                                                                textAlign =
                                                                                        TextAlign
                                                                                                .Center
                                                                        )
                                                                is ConversationState.LaunchingApp ->
                                                                        Text(
                                                                                targetState.message,
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyLarge,
                                                                                textAlign =
                                                                                        TextAlign
                                                                                                .Center
                                                                        )
                                                                is ConversationState.AppNotFound ->
                                                                        Text(
                                                                                "App not found. Try a different name.",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyLarge,
                                                                                textAlign =
                                                                                        TextAlign
                                                                                                .Center
                                                                        )
                                                                is ConversationState.MultipleAppsFound ->
                                                                        Text(
                                                                                targetState
                                                                                        .confirmationMessage,
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyLarge,
                                                                                textAlign =
                                                                                        TextAlign
                                                                                                .Center
                                                                        )
                                                                is ConversationState.Error ->
                                                                        Text(
                                                                                "Error: ${targetState.message}",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyLarge,
                                                                                textAlign =
                                                                                        TextAlign
                                                                                                .Center,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .error
                                                                        )
                                                        }
                                                }
                                        }
                                }

                                // Input Field (pinned to the bottom of this Column)
                                Column(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .padding(
                                                                horizontal = 8.dp
                                                        ) // Padding for the input area itself
                                ) {
                                        BasicTextField(
                                                value = inputText,
                                                onValueChange = {
                                                        launcherViewModel.onInputTextChanged(it)
                                                },
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .focusRequester(
                                                                        inputFocusRequester
                                                                ),
                                                textStyle =
                                                        LocalTextStyle.current.copy(
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface,
                                                                fontFamily = FrauncesFontFamily,
                                                                textAlign = TextAlign.Center,
                                                                fontSize =
                                                                        MaterialTheme.typography
                                                                                .titleMedium
                                                                                .fontSize // Slightly larger input text
                                                        ),
                                                keyboardOptions =
                                                        KeyboardOptions.Default.copy(
                                                                imeAction =
                                                                        when (inputMode) {
                                                                                InputMode
                                                                                        .APP_SEARCH ->
                                                                                        ImeAction
                                                                                                .Search
                                                                                else ->
                                                                                        ImeAction
                                                                                                .Done
                                                                        },
                                                                keyboardType =
                                                                        when (inputMode) {
                                                                                InputMode
                                                                                        .AWAITING_DURATION ->
                                                                                        KeyboardType
                                                                                                .Number
                                                                                else ->
                                                                                        KeyboardType
                                                                                                .Text
                                                                        },
                                                                capitalization =
                                                                        when (inputMode) {
                                                                                InputMode
                                                                                        .AWAITING_REASON ->
                                                                                        KeyboardCapitalization
                                                                                                .Sentences
                                                                                else ->
                                                                                        KeyboardCapitalization
                                                                                                .None
                                                                        }
                                                        ),
                                                keyboardActions =
                                                        KeyboardActions(
                                                                onSearch = {
                                                                        if (inputMode ==
                                                                                        InputMode
                                                                                                .APP_SEARCH
                                                                        )
                                                                                launcherViewModel
                                                                                        .submitInput()
                                                                },
                                                                onDone = {
                                                                        if (inputMode !=
                                                                                        InputMode
                                                                                                .APP_SEARCH
                                                                        )
                                                                                launcherViewModel
                                                                                        .submitInput()
                                                                }
                                                        ),
                                                singleLine = true,
                                                cursorBrush =
                                                        SolidColor(
                                                                MaterialTheme.colorScheme.primary
                                                        ),
                                                decorationBox = { innerTextField ->
                                                        Column(
                                                                horizontalAlignment =
                                                                        Alignment.CenterHorizontally
                                                        ) {
                                                                Box(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                                        .padding(
                                                                                                vertical =
                                                                                                        12.dp
                                                                                        ), // Padding for text
                                                                        contentAlignment =
                                                                                Alignment.Center
                                                                ) {
                                                                        if (inputText.isEmpty()) {
                                                                                val hintText =
                                                                                        when (inputMode
                                                                                        ) {
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
                                                                                        text =
                                                                                                hintText,
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
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurface
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.4f
                                                                                        ),
                                                                        thickness = 1.dp
                                                                )
                                                        }
                                                }
                                        )
                                }
                        }
                }
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableUsageCardItem(
        card: UsageCard,
        onDismissed: () -> Unit,
        modifier: Modifier = Modifier
) {
        val coroutineScope = rememberCoroutineScope()
        val offsetX = remember { Animatable(0f) }
        val alphaAnim = remember { Animatable(1f) } // Renamed to avoid conflict
        val localDensity = LocalDensity.current
        val dismissThreshold = remember { with(localDensity) { 120.dp.toPx() } }

        Box(
                modifier =
                        modifier.graphicsLayer(
                                        translationX = offsetX.value,
                                        alpha = alphaAnim.value
                                )
                                .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                                onDragEnd = {
                                                        coroutineScope.launch {
                                                                if (abs(offsetX.targetValue) >
                                                                                dismissThreshold
                                                                ) {
                                                                        offsetX.animateTo(
                                                                                targetValue =
                                                                                        if (offsetX.targetValue >
                                                                                                        0
                                                                                        )
                                                                                                size.width
                                                                                                        .toFloat()
                                                                                        else
                                                                                                -size.width
                                                                                                        .toFloat(),
                                                                                animationSpec =
                                                                                        tween(
                                                                                                durationMillis =
                                                                                                        300
                                                                                        )
                                                                        )
                                                                        alphaAnim.animateTo(
                                                                                0f,
                                                                                animationSpec =
                                                                                        tween(
                                                                                                durationMillis =
                                                                                                        250
                                                                                        )
                                                                        )
                                                                        onDismissed()
                                                                } else {
                                                                        offsetX.animateTo(
                                                                                0f,
                                                                                animationSpec =
                                                                                        tween(
                                                                                                durationMillis =
                                                                                                        200
                                                                                        )
                                                                        )
                                                                }
                                                        }
                                                },
                                                onHorizontalDrag = { change, dragAmount ->
                                                        change.consume()
                                                        coroutineScope.launch {
                                                                offsetX.snapTo(
                                                                        offsetX.value + dragAmount
                                                                )
                                                        }
                                                }
                                        )
                                }
        ) { UsageCardItem(card = card) }
}

@Composable
fun UsageCardItem(card: UsageCard) {
        val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

        fun formatActualDuration(duration: Duration?): String {
                if (duration == null) return "In progress"
                val totalSeconds = duration.seconds
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                return when {
                        hours > 0 ->
                                String.format("%dh %02dm", hours, minutes) // Simplified for card
                        minutes > 0 -> String.format("%dm %02ds", minutes, seconds)
                        else -> String.format("%ds", seconds)
                }
        }

        Surface(
                modifier = Modifier.fillMaxWidth(), // .padding(vertical = 4.dp) // Padding is on
                // SwipeableUsageCardItem
                shape = MaterialTheme.shapes.medium,
                color =
                        MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.15f
                        ), // Slightly more visible
                tonalElevation = 0.5.dp
        ) {
                Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) { // Inner padding for content
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top // Align app name and time to top
                        ) {
                                Text(
                                        text = card.appName,
                                        style =
                                                MaterialTheme.typography.titleMedium.copy(
                                                        fontFamily = FrauncesFontFamily
                                                ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                        text = card.openTime.format(timeFormatter),
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        alpha = 0.8f
                                                )
                                )
                        }

                        Spacer(modifier = Modifier.height(6.dp)) // Reduced spacer

                        if (card.reason.isNotBlank()) {
                                Text(
                                        text = card.reason, // Removed "Reason: " prefix for
                                        // cleaner look
                                        style =
                                                MaterialTheme.typography.bodyMedium.copy(
                                                        fontStyle = FontStyle.Italic
                                                ),
                                        color =
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        alpha = 0.9f
                                                ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp)) // Reduced spacer
                        } else {
                                // If no reason, add a bit of space to maintain some consistency in
                                // card height for short app names
                                Spacer(
                                        modifier =
                                                Modifier.height(
                                                        (MaterialTheme.typography
                                                                        .bodyMedium
                                                                        .lineHeight
                                                                        .value * 2)
                                                                .dp / 2
                                                )
                                ) // Approx 1 line
                        }

                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        text = "Planned: ${card.requestedDurationMinutes} min",
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        alpha = 0.8f
                                                )
                                )
                                Text(
                                        text = "Used: ${formatActualDuration(card.actualDuration)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        alpha = 0.8f
                                                )
                                )
                        }
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
                                        MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.1f
                                        ), // Match card bg type
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
                                        ) // Ensure tiny segments are visible
                                Box(
                                        modifier =
                                                Modifier.fillMaxHeight()
                                                        .weight(segmentWeight)
                                                        .background(
                                                                MaterialTheme.colorScheme.primary
                                                                        .copy(
                                                                                alpha = 0.6f
                                                                        ), // More vibrant
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
                                        ) // Subtle spacer
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
                                OutlinedTextField( // Kept OutlinedTextField here as it's a dialog
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
fun ConfirmAppSelectionDialog( // This seems like MultipleAppsFoundDialog, maybe can be consolidated
        // if same UI
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

fun formatDuration(duration: Duration): String { // General helper
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        return when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                else -> "${minutes}m"
        }
}
