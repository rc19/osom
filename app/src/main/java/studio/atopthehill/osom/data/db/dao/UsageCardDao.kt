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
}
