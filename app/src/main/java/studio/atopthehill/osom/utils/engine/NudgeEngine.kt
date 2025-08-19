package studio.atopthehill.osom.utils.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import studio.atopthehill.osom.data.db.entity.TaskStatus
import studio.atopthehill.osom.data.repository.AppRepository
import studio.atopthehill.osom.utils.managers.NudgeManager

class NudgeEngine(
    private val appRepository: AppRepository,
    private val nudgeManager: NudgeManager,
    private val foregroundApp: Flow<String>
) {
    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        var lastPackageName: String? = null

        engineScope.launch {
            // Observe foreground app for active notifications
            foregroundApp.collect { packageName ->
                lastPackageName?.let { lastPackage ->
                    if (packageName != lastPackage) {
                        launch {
                            val whitelistedApps = appRepository.allInstalledApps.first().filter { it.isWhitelisted }
                            val wasWhitelisted = whitelistedApps.any { it.packageName == lastPackage }

                            if (wasWhitelisted) {
                                val lastTask = appRepository.getLatestUsageCardForPackage(lastPackage)
                                if (lastTask != null && lastTask.status == TaskStatus.PENDING) {
                                    nudgeManager.showActiveNotification(lastTask.id, "Finished with ${lastTask.appName} for now?")
                                }
                            }
                        }
                    }
                }
                lastPackageName = packageName
            }
        }
    }

    fun stop() {
        engineScope.cancel()
    }
}
