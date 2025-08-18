package studio.atopthehill.osom.data.db.dao // Package declaration for the DAO

import androidx.room.Dao // Room annotation for Data Access Object
import androidx.room.Insert // Room annotation for insert operations
import androidx.room.OnConflictStrategy // Room conflict strategy for inserts
import androidx.room.Query // Room annotation for custom SQL queries
import kotlinx.coroutines.flow.Flow // Kotlin Coroutines Flow for asynchronous data streams
import studio.atopthehill.osom.data.db.entity.AppInfo // Importing the AppInfo entity

@Dao // Marks the interface as a Data Access Object
interface AppInfoDao { // Defines the DAO interface for AppInfo entities

        @Insert(
                onConflict = OnConflictStrategy.REPLACE
        ) // Annotation for inserting AppInfo, replaces on conflict
        suspend fun insertOrUpdateApp(
                appInfo: AppInfo
        ) // Suspended function to insert or update a single app

        @Insert(
                onConflict = OnConflictStrategy.REPLACE
        ) // Annotation for inserting a list of AppInfo, replaces on conflict
        suspend fun insertOrUpdateAllApps(
                apps: List<AppInfo>
        ) // Suspended function to insert or update a list of apps

        @Query(
                "SELECT * FROM app_info WHERE package_name = :packageName"
        ) // SQL query to select an app by its package name
        suspend fun getAppByPackageName(
                packageName: String
        ): AppInfo? // Suspended function to get a specific app, returns nullable AppInfo

        @Query(
                "SELECT * FROM app_info ORDER BY label ASC"
        ) // SQL query to select all apps, ordered by label
        fun getAllApps():
                Flow<List<AppInfo>> // Function returning a Flow of AppInfo list for reactive
        // updates

        @Query(
                "SELECT * FROM app_info WHERE is_installed = 1 ORDER BY label ASC"
        ) // SQL query to select only installed apps, ordered by label
        fun getInstalledApps():
                Flow<List<AppInfo>> // Function returning a Flow of installed AppInfo list

        @Query(
                "UPDATE app_info SET is_installed = 0, last_updated = :timestamp WHERE package_name = :packageName"
        ) // SQL query to mark an app as uninstalled
        suspend fun markAsUninstalled(
                packageName: String,
                timestamp: Long = System.currentTimeMillis()
        ) // Suspended function to update an app's installed status to false

        @Query(
                "UPDATE app_info SET is_installed = 1, last_updated = :timestamp WHERE package_name = :packageName"
        ) // SQL query to mark an app as installed
        suspend fun markAsInstalled(
                packageName: String,
                timestamp: Long = System.currentTimeMillis()
        ) // Suspended function to update an app's installed status to true

        @Query(
                "DELETE FROM app_info WHERE package_name = :packageName"
        ) // SQL query to delete an app by its package name
        suspend fun deleteApp(
                packageName: String
        ) // Suspended function to delete a specific app (consider if this is needed per rules,
        // rules
        // say "Mark apps as uninstalled (don't delete)")

        @Query("DELETE FROM app_info") // SQL query to delete all apps from the table
        suspend fun clearAllApps() // Suspended function to clear the entire app_info table

        @Query("UPDATE app_info SET is_whitelisted = :isWhitelisted WHERE package_name = :packageName")
        suspend fun setWhitelisted(packageName: String, isWhitelisted: Boolean)
}
