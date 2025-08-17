package studio.atopthehill.osom.ui.today

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.atopthehill.osom.data.db.entity.TaskStatus
import studio.atopthehill.osom.data.db.entity.UsageCard
import studio.atopthehill.osom.ui.theme.OSOMTheme
import java.time.LocalDateTime

@Composable
fun TodayScreen(
    tasks: List<UsageCard>,
    onAddTask: () -> Unit,
    onTaskCompleted: (UsageCard) -> Unit,
    onTaskSnoozed: (UsageCard) -> Unit,
    onTaskDismissed: (UsageCard) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Filled.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (tasks.isEmpty()) {
                EmptyState()
            } else {
                TaskList(
                    tasks = tasks,
                    onTaskCompleted = onTaskCompleted,
                    onTaskSnoozed = onTaskSnoozed,
                    onTaskDismissed = onTaskDismissed
                )
            }
        }
    }
}

@Composable
fun TaskList(
    tasks: List<UsageCard>,
    onTaskCompleted: (UsageCard) -> Unit,
    onTaskSnoozed: (UsageCard) -> Unit,
    onTaskDismissed: (UsageCard) -> Unit
) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(tasks) { task ->
            TaskCard(
                task = task,
                onCompleted = { onTaskCompleted(task) },
                onSnooze = { onTaskSnoozed(task) },
                onDismiss = { onTaskDismissed(task) }
            )
        }
    }
}

@Composable
fun TaskCard(
    task: UsageCard,
    onCompleted: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.status == TaskStatus.COMPLETED,
                onCheckedChange = { onCompleted() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "from ${task.appName} • ${task.timestamp}", // Placeholder for formatted timestamp
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onSnooze) {
                Icon(Icons.Filled.Alarm, contentDescription = "Snooze")
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss")
            }
        }
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "Magnifying glass",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "You’re all set!",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Osom is now active. As you use your other apps, I’ll start looking for tasks and reminders.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TodayScreenPreview_Empty() {
    OSOMTheme {
        TodayScreen(
            tasks = emptyList(),
            onAddTask = {},
            onTaskCompleted = {},
            onTaskSnoozed = {},
            onTaskDismissed = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TodayScreenPreview_WithTasks() {
    OSOMTheme {
        TodayScreen(
            tasks = listOf(
                UsageCard(1, "WhatsApp", "com.whatsapp", LocalDateTime.now(), "Follow up with Priya about the report", TaskStatus.PENDING),
                UsageCard(2, "Email", "com.google.android.gm", LocalDateTime.now().minusDays(1), "Call the bank tomorrow morning", TaskStatus.PENDING),
                UsageCard(3, "Messages", "com.google.android.apps.messaging", LocalDateTime.now().minusDays(1), "Pick up dry cleaning", TaskStatus.COMPLETED)
            ),
            onAddTask = {},
            onTaskCompleted = {},
            onTaskSnoozed = {},
            onTaskDismissed = {}
        )
    }
}