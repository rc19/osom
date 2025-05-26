package studio.atopthehill.osom.data.db

// import androidx.room.migration.Migration // Will be needed if not using fallback
// import androidx.sqlite.db.SupportSQLiteDatabase // Will be needed if not using fallback
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import studio.atopthehill.osom.data.db.dao.AppInfoDao
import studio.atopthehill.osom.data.db.dao.AppUsageDao
import studio.atopthehill.osom.data.db.dao.UsageCardDao
import studio.atopthehill.osom.data.db.dao.UserStatsDao
import studio.atopthehill.osom.data.db.entity.AppInfo
import studio.atopthehill.osom.data.db.entity.AppUsage
import studio.atopthehill.osom.data.db.entity.UsageCard
import studio.atopthehill.osom.data.db.entity.UserStats

@Database(
        entities = [AppInfo::class, AppUsage::class, UsageCard::class, UserStats::class],
        version = 2, // Incremented version
        exportSchema = true // Recommended to set to true and manage schemas
)
@TypeConverters(Converters::class) // Added TypeConverters
abstract class AppDatabase : RoomDatabase() { // Abstract class for the Room database

    abstract fun appInfoDao(): AppInfoDao // Abstract method to get AppInfoDao
    abstract fun appUsageDao(): AppUsageDao // Abstract method to get AppUsageDao
    abstract fun usageCardDao(): UsageCardDao // Added DAO for UsageCard
    abstract fun userStatsDao(): UserStatsDao // Added DAO for UserStats

    companion object { // Companion object for singleton instance
        @Volatile // Ensures visibility of changes to other threads
        private var INSTANCE: AppDatabase? = null // Singleton instance of the database

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
                    ?: synchronized(this) { // Synchronized block to ensure thread safety
                        val instance =
                                Room.databaseBuilder( // Build the database instance
                                                context.applicationContext,
                                                AppDatabase::class.java,
                                                "osom_launcher_database" // Name of the database
                                                // file
                                                )
                                        .fallbackToDestructiveMigration() // Added for simplicity
                                        // during dev
                                        // .addMigrations(MIGRATION_1_2) // Use this if providing
                                        // specific migrations
                                        .build()
                        INSTANCE = instance // Assign the built instance
                        instance // Return the instance
                    }
        }
    }
}
