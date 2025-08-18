package studio.atopthehill.osom.ui.today

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import studio.atopthehill.osom.data.db.entity.TaskStatus
import studio.atopthehill.osom.data.db.entity.UsageCard
import studio.atopthehill.osom.data.repository.AppRepository
import java.time.LocalDateTime
import org.junit.Assert.assertEquals

@ExperimentalCoroutinesApi
class TodayViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: TodayViewModel
    private lateinit var appRepository: AppRepository
    private lateinit var application: Application

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = mock()
        appRepository = mock()
        whenever(appRepository.getPendingTasks()).thenReturn(flowOf(emptyList()))
        whenever(appRepository.getCompletedTasks()).thenReturn(flowOf(emptyList()))
        viewModel = TodayViewModel(application, appRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onTaskCompleted should call repository to update task completion`() = runTest {
        // Given
        val task = UsageCard(1, "Test App", "com.test", LocalDateTime.now(), "Test Task")

        // When
        viewModel.onTaskCompleted(task)
        testDispatcher.scheduler.advanceUntilIdle() // Ensure coroutine completes

        // Then
        verify(appRepository).updateTaskCompletion(eq(task.id), eq(TaskStatus.COMPLETED), any())
    }

    @Test
    fun `onTaskSnoozed should call repository to update task timestamp`() = runTest {
        // Given
        val task = UsageCard(1, "Test App", "com.test", LocalDateTime.now(), "Test Task")

        // When
        viewModel.onTaskSnoozed(task)
        testDispatcher.scheduler.advanceUntilIdle() // Ensure coroutine completes

        // Then
        verify(appRepository).updateTaskTimestamp(eq(task.id), any())
    }

    @Test
    fun `onTaskDismissed should call repository to update task status`() = runTest {
        // Given
        val task = UsageCard(1, "Test App", "com.test", LocalDateTime.now(), "Test Task")

        // When
        viewModel.onTaskDismissed(task)
        testDispatcher.scheduler.advanceUntilIdle() // Ensure coroutine completes

        // Then
        verify(appRepository).updateTaskStatus(task.id, TaskStatus.DISMISSED)
    }

    @Test
    fun `init should load pending and completed tasks`() = runTest {
        // Given
        val pendingTasks = listOf(UsageCard(1, "Pending", "com.pending", LocalDateTime.now(), "Pending Task"))
        val completedTasks = listOf(UsageCard(2, "Completed", "com.completed", LocalDateTime.now(), "Completed Task", status = TaskStatus.COMPLETED))
        whenever(appRepository.getPendingTasks()).thenReturn(flowOf(pendingTasks))
        whenever(appRepository.getCompletedTasks()).thenReturn(flowOf(completedTasks))

        // When
        viewModel = TodayViewModel(application, appRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(pendingTasks, viewModel.pendingTasks.value)
        assertEquals(completedTasks, viewModel.completedTasks.value)
    }
}
