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

class TodayViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository: AppRepository =
        (application as OsomApplication).appRepository

    private val _tasks = MutableStateFlow<List<UsageCard>>(emptyList())
    val tasks: StateFlow<List<UsageCard>> = _tasks.asStateFlow()

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            // This will be connected to the DAO in the next step
            appRepository.getPendingTasks().collect { taskList ->
                _tasks.value = taskList
            }
        }
    }

    fun onTaskCompleted(task: UsageCard) {
        viewModelScope.launch {
            appRepository.updateTaskStatus(task.id, TaskStatus.COMPLETED)
        }
    }

    fun onTaskSnoozed(task: UsageCard) {
        viewModelScope.launch {
            // TODO: Implement a dialog to select snooze duration
            val snoozeUntil = LocalDateTime.now().plusHours(1)
            appRepository.updateTaskSnoozeStatus(task.id, TaskStatus.SNOOZED, snoozeUntil)
        }
    }

    fun onTaskDismissed(task: UsageCard) {
        viewModelScope.launch {
            appRepository.updateTaskStatus(task.id, TaskStatus.DISMISSED)
        }
    }

    fun onAddTask() {
        // TODO: Implement navigation to the add task screen
    }
}
