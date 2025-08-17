package studio.atopthehill.osom.ui.summary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.time.Duration
import java.time.format.DateTimeFormatter
import studio.atopthehill.osom.OsomApplication
import studio.atopthehill.osom.data.db.entity.UsageCard
import studio.atopthehill.osom.ui.launcher.LauncherViewModel
import studio.atopthehill.osom.ui.launcher.LauncherViewModelFactory
import studio.atopthehill.osom.ui.theme.EInkTextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
        navController: NavController,
        launcherViewModel: LauncherViewModel =
                viewModel(
                        factory =
                                LauncherViewModelFactory(
                                        LocalContext.current.applicationContext as OsomApplication
                                )
                )
) {
        val allUsageCards by launcherViewModel.allUsageCards.collectAsState()

        LaunchedEffect(Unit) {
                launcherViewModel.loadAllUsageCards() // Ensure data is loaded when screen appears
        }

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = { Text("Usage Summary") },
                                navigationIcon = {
                                        IconButton(onClick = { navController.popBackStack() }) {
                                                Icon(
                                                        Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = "Back"
                                                )
                                        }
                                }
                        )
                }
        ) { paddingValues ->
                if (allUsageCards.isEmpty()) {
                        Box(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .padding(paddingValues)
                                                .padding(16.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                        ) { Text("No usage data yet.") }
                } else {
                        LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(paddingValues),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                items(allUsageCards, key = { card -> card.id }) { card ->
                                        UsageCardSummaryItem(card = card)
                                }
                        }
                }
        }
}

@Composable
fun UsageCardSummaryItem(card: UsageCard) {
        val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm, MMM dd") }

        fun formatDurationForSummary(duration: Duration?): String {
                if (duration == null) return "In progress"
                if (duration.isZero) return "< 1 min"
                val totalSeconds = duration.seconds
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                return when {
                        hours > 0 -> String.format("%dh %02dm", hours, minutes)
                        minutes > 0 -> String.format("%dm", minutes)
                        else -> "< 1 min" // Should be caught by isZero, but as fallback
                }
        }

        Card(
                modifier =
                        Modifier.fillMaxWidth()
                                .border(
                                        BorderStroke(1.dp, EInkTextPrimary),
                                        shape = RoundedCornerShape(8.dp)
                                ),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                                text = card.appName,
                                style = MaterialTheme.typography.titleMedium,
                                color = EInkTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                                text = "Opened: ${card.timestamp.format(timeFormatter)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = EInkTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (card.title.isNotBlank() && card.title != "System-detected usage") {
                                Text(
                                        text = "Title: ${card.title}",
                                        style =
                                                MaterialTheme.typography.bodyMedium.copy(
                                                        fontStyle = FontStyle.Italic
                                                ),
                                        maxLines = 3,
                                        color = EInkTextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                        }

                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Text(
                                        text =
                                                "Planned: ${card.requestedDurationMinutes?.let { "$it min" } ?: "N/A"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EInkTextPrimary
                                )
                                Text(
                                        text =
                                                "Used: ${formatDurationForSummary(card.actualDuration)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EInkTextPrimary
                                )
                        }
                }
        }
}
