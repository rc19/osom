package studio.atopthehill.osom.ui.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import studio.atopthehill.osom.OsomApplication
import studio.atopthehill.osom.data.db.entity.UsageCard
import studio.atopthehill.osom.data.db.entity.TaskStatus
import studio.atopthehill.osom.data.repository.AppRepository
import java.time.LocalDateTime

class TodayViewModel(
    application: Application,
    private val appRepository: AppRepository
) : AndroidViewModel(application) {

    private val _pendingTasks = MutableStateFlow<List<UsageCard>>(emptyList())
    val pendingTasks: StateFlow<List<UsageCard>> = _pendingTasks.asStateFlow()

    private val _completedTasks = MutableStateFlow<List<UsageCard>>(emptyList())
    val completedTasks: StateFlow<List<UsageCard>> = _completedTasks.asStateFlow()

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            appRepository.getPendingTasks().collect { taskList ->
                _pendingTasks.value = taskList
            }
        }
        viewModelScope.launch {
            appRepository.getCompletedTasks().collect { taskList ->
                _completedTasks.value = taskList
            }
        }
    }

    fun onTaskCompleted(task: UsageCard) {
        viewModelScope.launch {
            try {
                appRepository.updateTaskCompletion(task.id, TaskStatus.COMPLETED, LocalDateTime.now())
            } catch (e: Exception) {
                // TODO: Handle error (e.g., show a snackbar)
            }
        }
    }

    fun onTaskSnoozed(task: UsageCard) {
        viewModelScope.launch {
            try {
                appRepository.updateTaskTimestamp(task.id, LocalDateTime.now())
            } catch (e: Exception) {
                // TODO: Handle error
            }
        }
    }

    fun onTaskDismissed(task: UsageCard) {
        viewModelScope.launch {
            try {
                appRepository.updateTaskStatus(task.id, TaskStatus.DISMISSED)
            } catch (e: Exception) {
                // TODO: Handle error
            }
        }
    }

    fun onAddTask() {
        // TODO: Implement navigation to the add task screen
    }
}
