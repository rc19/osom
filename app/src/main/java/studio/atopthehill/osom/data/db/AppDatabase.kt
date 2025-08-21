package studio.atopthehill.osom.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
// AppUsageDao import will be removed by tool if not used
// AppUsage entity import will be removed by tool if not used
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import studio.atopthehill.osom.data.db.dao.AppInfoDao
import studio.atopthehill.osom.data.db.dao.UsageCardDao
import studio.atopthehill.osom.data.db.dao.UserStatsDao
import studio.atopthehill.osom.data.db.entity.AppInfo
import studio.atopthehill.osom.data.db.entity.UsageCard
import studio.atopthehill.osom.data.db.entity.UserStats

@Database(
        entities =
                [
                        AppInfo::class, /*AppUsage::class,*/
                        UsageCard::class,
                        UserStats::class], // AppUsage removed
        version = 9, // Incremented version from 8 to 9 for AI tasks feature
        exportSchema = true // Recommended to set to true and manage schemas
)
@TypeConverters(Converters::class) // Added TypeConverters
abstract class AppDatabase : RoomDatabase() { // Abstract class for the Room database

    abstract fun appInfoDao(): AppInfoDao // Abstract method to get AppInfoDao
    // abstract fun appUsageDao(): AppUsageDao // Abstract method to get AppUsageDao - REMOVED
    abstract fun usageCardDao(): UsageCardDao // Added DAO for UsageCard
    abstract fun userStatsDao(): UserStatsDao // Added DAO for UserStats

    companion object { // Companion object for singleton instance
        @Volatile // Ensures visibility of changes to other threads
        private var INSTANCE: AppDatabase? = null // Singleton instance of the database

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_stats ADD COLUMN activeReminders INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add enableAITasks column with default value false (0 in SQLite)
                database.execSQL("ALTER TABLE user_stats ADD COLUMN enableAITasks INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migration from 1 to 2 (Placeholder if not using fallbackToDestructiveMigration)
        // val MIGRATION_1_2 = object : Migration(1, 2) {
        //     override fun migrate(database: SupportSQLiteDatabase) {
        //         // Create new tables. SQL needs to be precise.
        //         database.execSQL(
        // "CREATE TABLE IF NOT EXISTS `usage_cards` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT
        // NULL, `appName` TEXT NOT NULL, `packageName` TEXT NOT NULL, `openTime` TEXT NOT NULL,
        // `requestedDurationMinutes` INTEGER NOT NULL, `actualDuration` TEXT, `reason` TEXT NOT
        // NULL, `timestamp` TEXT NOT NULL)"
        //         )
        //         database.execSQL(
        // "CREATE TABLE IF NOT EXISTS `user_stats` (`id` INTEGER PRIMARY KEY NOT NULL,
        // `dailyInteractions` INTEGER NOT NULL, `totalUsageToday` TEXT NOT NULL, `lastInteraction`
        // TEXT NOT NULL, `userName` TEXT)"
        //         )
        //     }
        // }

        fun getDatabase(context: Context): AppDatabase { // Function to get the database instance
            return INSTANCE
                    ?: synchronized(this) { // Singleton pattern with thread safety
                        val instance =
                                Room.databaseBuilder(
                                                context.applicationContext,
                                                AppDatabase::class.java,
                                                "osom_database" // Name of the database file
                                        )
                                        .addMigrations(MIGRATION_7_8, MIGRATION_8_9) // Add migrations if not
                                        // .fallbackToDestructiveMigration() // Destroys and recreates
                                        // if no migration found -
                                        // USE WITH CAUTION
                                        .build()
                        INSTANCE = instance
                        instance
                    }
        }
    }
}
