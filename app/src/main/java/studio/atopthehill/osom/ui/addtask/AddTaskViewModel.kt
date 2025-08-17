package studio.atopthehill.osom.ui.addtask

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import studio.atopthehill.osom.OsomApplication
import studio.atopthehill.osom.data.db.entity.UsageCard
import studio.atopthehill.osom.data.repository.AppRepository
import java.time.LocalDateTime

class AddTaskViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository: AppRepository =
        (application as OsomApplication).appRepository

    fun saveTask(title: String) {
        viewModelScope.launch {
            val task = UsageCard(
                appName = "Manual",
                packageName = "studio.atopthehill.osom",
                timestamp = LocalDateTime.now(),
                title = title
            )
            appRepository.insertUsageCard(task)
        }
    }
}
