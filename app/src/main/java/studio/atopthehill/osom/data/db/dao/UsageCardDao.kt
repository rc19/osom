package studio.atopthehill.osom.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import studio.atopthehill.osom.data.db.entity.UsageCard

@Dao
interface UsageCardDao {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertUsageCard(usageCard: UsageCard): Long

        @Update suspend fun updateUsageCard(usageCard: UsageCard)

        // Get all usage cards, ordered by timestamp (most recent first)
        @Query("SELECT * FROM usage_cards ORDER BY timestamp DESC")
        fun getAllUsageCards(): Flow<List<UsageCard>>

        // Get all pending tasks, ordered by timestamp (most recent first)
        @Query("SELECT * FROM usage_cards WHERE status = 'PENDING' AND (snoozeUntil IS NULL OR snoozeUntil <= :currentTime) ORDER BY timestamp ASC")
        fun getPendingTasks(currentTime: LocalDateTime): Flow<List<UsageCard>>

        @Query("SELECT * FROM usage_cards WHERE status = 'COMPLETED' ORDER BY completionTimestamp DESC")
        fun getCompletedTasks(): Flow<List<UsageCard>>

        // Get usage cards for a specific day, ordered by timestamp (most recent first)
        @Query(
                "SELECT * FROM usage_cards WHERE DATE(timestamp) = DATE(:day) ORDER BY timestamp DESC"
        )
        fun getUsageCardsForDay(day: String): Flow<List<UsageCard>> // day as 'YYYY-MM-DD' string

        // Get usage cards within a specific date range
        @Query(
                "SELECT * FROM usage_cards WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC"
        )
        fun getUsageCardsBetween(
                startTime: LocalDateTime,
                endTime: LocalDateTime
        ): Flow<List<UsageCard>>

        // Get a specific usage card by its ID
        @Query("SELECT * FROM usage_cards WHERE id = :id")
        suspend fun getUsageCardById(id: Long): UsageCard?

        @Query("UPDATE usage_cards SET status = :status WHERE id = :id")
        suspend fun updateTaskStatus(id: Long, status: String)

        @Query("UPDATE usage_cards SET status = :status, snoozeUntil = :snoozeUntil WHERE id = :id")
        suspend fun updateTaskSnoozeStatus(id: Long, status: String, snoozeUntil: LocalDateTime?)

        @Query("UPDATE usage_cards SET status = :status, completionTimestamp = :completionTimestamp WHERE id = :id")
        suspend fun updateTaskCompletion(id: Long, status: String, completionTimestamp: LocalDateTime?)

        @Query("UPDATE usage_cards SET timestamp = :timestamp WHERE id = :id")
        suspend fun updateTaskTimestamp(id: Long, timestamp: LocalDateTime)

        // Get currently active (those without an actualDuration set) usage cards
        // This might be useful for checking if an app's time limit is still running
        @Query("SELECT * FROM usage_cards WHERE actualDuration IS NULL ORDER BY timestamp DESC")
        fun getActiveUsageCards(): Flow<List<UsageCard>>

        @Query("DELETE FROM usage_cards WHERE id = :id") suspend fun deleteUsageCardById(id: Long)

        @Query("DELETE FROM usage_cards") suspend fun clearAllUsageCards()

        // New query for backfill duplicate checking
        @Query(
                """
            SELECT * FROM usage_cards
            WHERE packageName = :packageName
              AND STRFTIME('%s', timestamp) * 1000 BETWEEN (:timestampMillis - :toleranceMillis) AND (:timestampMillis + :toleranceMillis)
        """
        )
        fun getCardsForAppAroundTime(
                packageName: String,
                timestampMillis: Long,
                toleranceMillis: Long
        ): Flow<List<UsageCard>>

        @Query("SELECT * FROM usage_cards WHERE packageName = :packageName ORDER BY timestamp DESC LIMIT 1")
        suspend fun getLatestUsageCardForPackage(packageName: String): UsageCard?
}
