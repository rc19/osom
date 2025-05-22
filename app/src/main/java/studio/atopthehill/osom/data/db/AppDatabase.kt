package studio.atopthehill.osom.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import studio.atopthehill.osom.data.db.dao.AppInfoDao
import studio.atopthehill.osom.data.db.dao.AppUsageDao
import studio.atopthehill.osom.data.db.entity.AppInfo
import studio.atopthehill.osom.data.db.entity.AppUsage

@Database(entities = [AppInfo::class, AppUsage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() { // Abstract class for the Room database

    abstract fun appInfoDao(): AppInfoDao // Abstract method to get AppInfoDao
    abstract fun appUsageDao(): AppUsageDao // Abstract method to get AppUsageDao

    companion object { // Companion object for singleton instance
        @Volatile // Ensures visibility of changes to other threads
        private var INSTANCE: AppDatabase? = null // Singleton instance of the database

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
                                        // .fallbackToDestructiveMigration() // Optional: Handle
                                        // migrations by destroying and recreating the DB (use with
                                        // caution)
                                        .build()
                        INSTANCE = instance // Assign the built instance
                        instance // Return the instance
                    }
        }
    }
}
