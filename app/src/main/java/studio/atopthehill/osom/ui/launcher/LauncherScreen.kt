package studio.atopthehill.osom.ui.launcher // Package for launcher specific UI components

// found
// awareness
import android.app.Activity // For context casting
import android.app.Application // For ViewModel Factory
import android.content.Context // For context
import android.content.ContextWrapper // For context traversal
import androidx.compose.foundation.clickable // Clickable modifier
import androidx.compose.foundation.layout.* // Layout components (Column, Row, Spacer, etc.)
import androidx.compose.foundation.lazy.LazyColumn // For displaying a list of apps if multiple are
import androidx.compose.foundation.lazy.items // For LazyColumn items
import androidx.compose.foundation.text.KeyboardActions // Keyboard actions for TextField
import androidx.compose.foundation.text.KeyboardOptions // Keyboard options for TextField
import androidx.compose.material3.*
import androidx.compose.runtime.* // Composable, remember, State, etc.
import androidx.compose.ui.Alignment // Alignment modifiers
import androidx.compose.ui.Modifier // Modifier for UI elements
import androidx.compose.ui.platform.LocalContext // To get current context
import androidx.compose.ui.platform.LocalFocusManager // To manage focus
import androidx.compose.ui.platform.LocalLifecycleOwner // To get lifecycle owner
import androidx.compose.ui.text.input.ImeAction // IME action for TextField (e.g., Search or Done)
import androidx.compose.ui.text.input.KeyboardCapitalization // Keyboard capitalization
import androidx.compose.ui.text.input.KeyboardType // Keyboard type (e.g., Text)
import androidx.compose.ui.unit.dp // Density-independent pixels for spacing and sizing
import androidx.compose.ui.window.Dialog // For reason prompt dialog
import androidx.lifecycle.Lifecycle // For lifecycle events
import androidx.lifecycle.ViewModelProvider // For ViewModel Factory
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Collect StateFlow with lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel // To get ViewModel instance
import studio.atopthehill.osom.OsomApplication // Custom Application class
import studio.atopthehill.osom.data.db.entity.AppInfo // AppInfo entity

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

@Composable
fun LauncherScreen(
        // ViewModel is typically provided via viewModel() delegate from
        // androidx.lifecycle.viewmodel.compose
        // For this to work with a custom factory (if needed later) or for AndroidViewModel, it
        // usually handles it.
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

    val focusManager = LocalFocusManager.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Effect to clear chat state based on lifecycle (e.g., onResume)
    // This handles Rule 1: "clear every time a user comes back to the homescreen..."
    DisposableEffect(lifecycleOwner) {
        val observer =
                androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        launcherViewModel.resetLauncherState()
                    }
                }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Main UI column
    Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom // Aligns chat input to the bottom
    ) {
        // Area for displaying search results or messages
        Box(
                modifier =
                        Modifier.weight(1f) // Takes up available space above the input
                                .fillMaxWidth(),
                contentAlignment = Alignment.Center
        ) {
            when (val state = conversationState) {
                is ConversationState.Idle -> {
                    // Nothing to show or a welcome message
                    // Text("Type an app name to start")
                }
                is ConversationState.Searching -> {
                    Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) { // Ensure text is below indicator
                        CircularProgressIndicator()
                        Text("Searching...", modifier = Modifier.padding(top = 8.dp))
                    }
                }
                is ConversationState.AppFound -> {
                    // Display the message asking for a reason
                    Text(state.askReasonMessage)
                }
                is ConversationState.AppNotFound -> {
                    Text("App not found. Try a different name or check if it's installed.")
                }
                is ConversationState.MultipleAppsFound -> {
                    Text(state.confirmationMessage)
                }
                is ConversationState.Error -> {
                    Text("Error: ${state.message}")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Text input field
        OutlinedTextField(
                value = inputText,
                onValueChange = { launcherViewModel.onInputTextChanged(it) },
                label = {
                    Text(if (inputMode == InputMode.APP_SEARCH) "Type app name..." else "Reason...")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions =
                        KeyboardOptions.Default.copy(
                                imeAction =
                                        if (inputMode == InputMode.APP_SEARCH) ImeAction.Search
                                        else ImeAction.Done,
                                keyboardType = KeyboardType.Text,
                                capitalization =
                                        if (inputMode == InputMode.APP_SEARCH)
                                                KeyboardCapitalization.None
                                        else KeyboardCapitalization.Sentences,
                                // autoCorrect = false // Still no autocorrect as per general rule
                        ),
                keyboardActions =
                        KeyboardActions(
                                onSearch = {
                                    if (inputMode == InputMode.APP_SEARCH) {
                                        launcherViewModel.submitInput()
                                        focusManager.clearFocus()
                                    }
                                },
                                onDone = {
                                    if (inputMode == InputMode.AWAITING_REASON) {
                                        launcherViewModel.submitInput()
                                        focusManager.clearFocus()
                                    }
                                }
                        )
        )
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
                                            capitalization = KeyboardCapitalization.Sentences,
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
                                                focusManager.clearFocus() // Hide keyboard
                                            }
                                    )
                    )
                }
            },
            confirmButton = {
                Button(
                        onClick = {
                            onConfirm(reasonText.ifBlank { "Opened ${appInfo.label}" })
                            focusManager.clearFocus() // Hide keyboard
                        },
                ) { Text("Open App") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// Optional: If you had a MultipleFound dialog
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
                        "Multiple apps found. Please select one:",
                        style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(apps) { app ->
                        Text(
                                text = app.label,
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .clickable { onAppSelected(app) }
                                                .padding(vertical = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        }
    }
}
