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
import studio.atopthehill.osom.data.repository.AppRepository

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
            // TODO: Implement task completion logic
        }
    }

    fun onTaskSnoozed(task: UsageCard) {
        viewModelScope.launch {
            // TODO: Implement task snoozing logic
        }
    }

    fun onTaskDismissed(task: UsageCard) {
        viewModelScope.launch {
            // TODO: Implement task dismissal logic
        }
    }

    fun onAddTask() {
        // TODO: Implement navigation to the add task screen
    }
}
