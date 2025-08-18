package studio.atopthehill.osom.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Represents the status of a task in the "Today" dashboard.
 * A task can be in one of the following states:
 * - PENDING: The task is active and needs to be completed.
 * - COMPLETED: The task has been marked as done by the user.
 * - SNOOZED: The user has temporarily postponed the task.
 * - DISMISSED: The user has removed the task from the active list.
 */
enum class TaskStatus {
    PENDING,
    COMPLETED,
    SNOOZED,
    DISMISSED
}

@Entity(tableName = "usage_cards")
data class UsageCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String,
    val packageName: String,
    val timestamp: LocalDateTime,
    val title: String, // The main description of the task or the reason for the usage card
    val status: TaskStatus = TaskStatus.PENDING, // The current status of the task
    var snoozeUntil: LocalDateTime? = null, // Timestamp until which the task is snoozed
    var completionTimestamp: LocalDateTime? = null, // Timestamp when the task was completed

    // Nullable fields for app usage tracking, not all cards are tasks
    val openTime: LocalTime? = null,
    val requestedDurationMinutes: Int? = null,
    var actualDuration: Duration? = null,
    var timerStartTime: Long? = null,
    var isTimerFinishedGracefully: Boolean? = null
)
