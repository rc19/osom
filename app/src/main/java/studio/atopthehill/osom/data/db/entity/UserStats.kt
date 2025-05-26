package studio.atopthehill.osom.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.LocalDateTime

@Entity(tableName = "user_stats")
data class UserStats(
        @PrimaryKey val id: Int = 1, // Singleton for user stats
        var dailyInteractions: Int,
        var totalUsageToday: Duration,
        var lastInteraction: LocalDateTime,
        var userName: String? = null // Added optional userName
)
