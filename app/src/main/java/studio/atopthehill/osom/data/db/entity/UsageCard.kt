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
        val requestedDurationMinutes: Int, // Storing the user's requested duration in minutes
        var actualDuration: Duration?, // To be filled when the app is closed or time expires
        val reason: String,
        val timestamp: LocalDateTime // Timestamp of when the card was created/app launched
)
