package studio.atopthehill.osom.ui.controlcenter

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import studio.atopthehill.osom.data.db.AppDatabase
import studio.atopthehill.osom.data.db.entity.AppInfo
import studio.atopthehill.osom.data.db.entity.UserStats
import studio.atopthehill.osom.data.repository.AppRepository

class ControlCenterViewModel(private val appRepository: AppRepository) : ViewModel() {

    val allApps: StateFlow<List<AppInfo>> = appRepository.allInstalledApps
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userStats: StateFlow<UserStats?> = appRepository.getUserStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun setAppWhitelisted(packageName: String, isWhitelisted: Boolean) {
        viewModelScope.launch {
            appRepository.setWhitelisted(packageName, isWhitelisted)
        }
    }

    fun setActiveReminders(isActive: Boolean) {
        viewModelScope.launch {
            val currentUserStats = userStats.value
            if (currentUserStats != null) {
                appRepository.insertOrUpdateUserStats(currentUserStats.copy(activeReminders = isActive))
            }
        }
    }

    fun setAITasksEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            appRepository.setAITasksEnabled(isEnabled)
        }
    }
}

class ControlCenterViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ControlCenterViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            @Suppress("UNCHECKED_CAST")
            return ControlCenterViewModel(
                AppRepository(
                    application,
                    database.appInfoDao(),
                    database.usageCardDao(),
                    database.userStatsDao()
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}