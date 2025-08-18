package studio.atopthehill.osom.ui.today

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import studio.atopthehill.osom.OsomApplication

class TodayViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodayViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodayViewModel(
                application,
                (application as OsomApplication).appRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
