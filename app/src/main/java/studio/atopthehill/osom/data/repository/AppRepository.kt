package studio.atopthehill.osom.data.repository // Package declaration for the repository

import android.content.Context // Android Context for accessing system services like PackageManager
import android.content.Intent // Intent for querying launchable apps
import android.graphics.Bitmap // Bitmap for handling app icons
import android.graphics.drawable.BitmapDrawable // Drawable to Bitmap conversion
import androidx.core.graphics.drawable.toBitmap // Extension function for Drawable to Bitmap
import java.io.ByteArrayOutputStream // For converting Bitmap to ByteArray
import kotlinx.coroutines.Dispatchers // Coroutine dispatcher for IO operations
import kotlinx.coroutines.flow.Flow // Kotlin Coroutines Flow for asynchronous data streams
import kotlinx.coroutines.withContext // Coroutine scope for switching dispatchers
import studio.atopthehill.osom.data.db.AppDatabase // The Room database instance
import studio.atopthehill.osom.data.db.dao.AppInfoDao // DAO for AppInfo entity
import studio.atopthehill.osom.data.db.dao.AppUsageDao // DAO for AppUsage entity
import studio.atopthehill.osom.data.db.entity.AppInfo // AppInfo entity
import studio.atopthehill.osom.data.db.entity.AppUsage // AppUsage entity

class AppRepository(
        private val context: Context
) { // Repository class, takes Context for PackageManager

        private val appInfoDao: AppInfoDao =
                AppDatabase.getDatabase(context).appInfoDao() // Instance of AppInfoDao
        private val appUsageDao: AppUsageDao =
                AppDatabase.getDatabase(context).appUsageDao() // Instance of AppUsageDao

        // --- AppInfo Operations ---

        val allInstalledApps: Flow<List<AppInfo>> =
                appInfoDao.getInstalledApps() // Flow of all installed apps

        suspend fun refreshInstalledApps() { // Function to refresh the list of installed apps in
                // the
                // database
                withContext(Dispatchers.IO) { // Perform in IO context
                        val packageManager = context.packageManager // Get PackageManager
                        val intent =
                                Intent(Intent.ACTION_MAIN, null)
                                        .apply { // Intent to find launchable apps
                                                addCategory(Intent.CATEGORY_LAUNCHER)
                                        }
                        val resolveInfoList =
                                packageManager.queryIntentActivities(
                                        intent,
                                        0
                                ) // Get list of launchable activities
                        val apps =
                                resolveInfoList.mapNotNull { resolveInfo
                                        -> // Map ResolveInfo to AppInfo
                                        val packageName = resolveInfo.activityInfo.packageName
                                        val label = resolveInfo.loadLabel(packageManager).toString()
                                        val iconDrawable = resolveInfo.loadIcon(packageManager)

                                        val iconByteArray: ByteArray? =
                                                try {
                                                        val bitmap =
                                                                (iconDrawable as? BitmapDrawable)
                                                                        ?.bitmap
                                                                        ?: iconDrawable.toBitmap(
                                                                                100,
                                                                                100
                                                                        ) // Convert drawable to
                                                        // bitmap (e.g. 100x100)
                                                        val stream = ByteArrayOutputStream()
                                                        bitmap.compress(
                                                                Bitmap.CompressFormat.PNG,
                                                                90,
                                                                stream
                                                        ) // Compress bitmap to PNG
                                                        stream.toByteArray() // Get byte array
                                                } catch (e: Exception) {
                                                        // Log error or handle missing/corrupt icon
                                                        null
                                                }

                                        AppInfo(
                                                packageName,
                                                label,
                                                iconByteArray,
                                                isInstalled = true,
                                                lastUpdated = System.currentTimeMillis()
                                        )
                                }
                        appInfoDao.insertOrUpdateAllApps(apps) // Insert or update all fetched apps

                        // Optionally, mark apps not in resolveInfoList as uninstalled (more complex
                        // logic
                        // needed here to compare with existing DB)
                }
        }

        suspend fun getAppByPackageName(
                packageName: String
        ): AppInfo? { // Get a specific app by package name
                return appInfoDao.getAppByPackageName(packageName)
        }

        suspend fun markAppAsUninstalled(packageName: String) { // Mark an app as uninstalled
                appInfoDao.markAsUninstalled(packageName)
        }

        suspend fun insertOrUpdateApp(appInfo: AppInfo) { // Insert or update a single app
                appInfoDao.insertOrUpdateApp(appInfo)
        }

        // --- AppUsage Operations ---

        suspend fun logAppUsage(packageName: String, reason: String) { // Log an app usage event
                val usage =
                        AppUsage(
                                packageName = packageName,
                                reason = reason
                        ) // Create AppUsage entity
                appUsageDao.insertAppUsage(usage) // Insert into database
        }

        fun getUsageForApp(
                packageName: String
        ): Flow<List<AppUsage>> { // Get usage history for an app
                return appUsageDao.getUsageForApp(packageName)
        }

        fun getAllUsageRecords(): Flow<List<AppUsage>> { // Get all usage records
                return appUsageDao.getAllUsageRecords()
        }
}
