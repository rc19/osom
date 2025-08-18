package studio.atopthehill.osom.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.LocalDateTime

@Entity(tableName = "user_stats")
data class UserStats(
        @PrimaryKey(autoGenerate = false) // Set to false if we only have one row for user stats
        val id: Int = 1, // Default ID for the single user stats row
        val dailyInteractions: Int,
        val totalUsageToday: Duration,
        val lastInteraction: LocalDateTime,
        val userName: String?,
        val dayStartTimeHour: Int = 3, // Hour of the day (0-23) when the 'day' officially starts
        val activeReminders: Boolean = true // Whether active reminders are enabled
)
