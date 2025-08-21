package studio.atopthehill.osom.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import studio.atopthehill.osom.data.db.entity.UserStats

@Dao
interface UserStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(userStats: UserStats)

    @Update suspend fun updateStats(userStats: UserStats)

    // Get the user stats (should only be one row with id = 1)
    @Query("SELECT * FROM user_stats WHERE id = 1") fun getUserStats(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun getUserStatsDirect(): UserStats? // For non-flow synchronous access if needed

    // Potentially, methods to update specific fields if needed frequently
    // e.g., @Query("UPDATE user_stats SET dailyInteractions = dailyInteractions + 1 WHERE id = 1")
    // suspend fun incrementDailyInteractions()
    
    // Update AI tasks setting specifically (for better performance than full entity update)
    @Query("UPDATE user_stats SET enableAITasks = :enabled WHERE id = 1")
    suspend fun setAITasksEnabled(enabled: Boolean)
    
    // Update active reminders setting specifically
    @Query("UPDATE user_stats SET activeReminders = :enabled WHERE id = 1")
    suspend fun setActiveReminders(enabled: Boolean)
}
