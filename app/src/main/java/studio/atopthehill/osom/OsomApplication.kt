package studio.atopthehill.osom // Adjust package name if different

import android.app.Application // Importing the base Application class
import kotlinx.coroutines.CoroutineScope // For launching coroutines
import kotlinx.coroutines.Dispatchers // For defining coroutine dispatchers
import kotlinx.coroutines.SupervisorJob // For creating a cancellable coroutine scope
import studio.atopthehill.osom.data.db.AppDatabase
import studio.atopthehill.osom.data.repository.AppRepository // Importing the AppRepository

class OsomApplication :
        Application() { // Custom Application class inheriting from Android's Application

    // Create a cancellable coroutine scope that is tied to the Application lifecycle
    // Use SupervisorJob so that if one child coroutine fails, others are not cancelled
    private val applicationScope =
            CoroutineScope(
                    SupervisorJob() + Dispatchers.Main
            ) // Coroutine scope for application-level tasks

    // Lazy initialization of the AppRepository. It will be created only when first accessed.
    // The database and repository are initialized using the application context.
    val appRepository: AppRepository by lazy { // Lazily initialized AppRepository instance
        val database = AppDatabase.getDatabase(applicationContext)
        AppRepository(
            applicationContext,
            database.appInfoDao(),
            database.usageCardDao(),
            database.userStatsDao()
        )
    }

    override fun onCreate() { // Called when the application is starting, before any other objects
        // have been created
        super.onCreate() // Always call the superclass's method
        // You could perform initial setup here, like the first app refresh, if desired.
        // For example, to ensure the app list is populated on first start:
        // applicationScope.launch(Dispatchers.IO) {
        //     appRepository.refreshInstalledApps()
        // }
        // For now, we will let the ViewModel or specific features trigger the refresh as needed.
    }

    // Companion object to provide a static way to get the repository from the Application instance
    // This is a simple way to make the repository accessible, though dependency injection
    // frameworks
    // like Hilt would offer more robust solutions for larger apps.
    companion object { // Companion object for static access
        // Optional: A way to get the application instance itself if needed elsewhere, though direct
        // access to repository is preferred.
        // @get:Synchronized
        // lateinit var instance: OsomApplication
        //     private set

        // It's generally better to retrieve the repository via a context that can get the
        // Application,
        // or pass the repository instance directly, rather than holding a static instance here if
        // not careful about lifecycle.
        // The `appRepository` property on an instance of `OsomApplication` is the primary way to
        // access it.
    }
}
