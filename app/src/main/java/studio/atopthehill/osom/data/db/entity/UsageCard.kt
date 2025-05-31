package studio.atopthehill.osom.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(tableName = "usage_cards")
data class UsageCard(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val appName: String,
        val packageName: String, // Added packageName to uniquely identify the app
        val openTime: LocalTime,
        val requestedDurationMinutes: Int?, // Nullable: Duration requested by user in minutes
        var actualDuration: Duration? =
                null, // Actual duration used, null if still active or not set
        val reason: String,
        val timestamp: LocalDateTime, // Timestamp of when this card was created/event occurred
        var timerStartTime: Long? = null, // System.currentTimeMillis when timer actually started
        var isTimerFinishedGracefully: Boolean? =
                null // True if timer finished, false if cancelled by user returning, null if not
// set
)
