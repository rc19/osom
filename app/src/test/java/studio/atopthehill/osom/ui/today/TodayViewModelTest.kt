package studio.atopthehill.osom.ui.today

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import studio.atopthehill.osom.data.db.entity.TaskStatus
import studio.atopthehill.osom.data.db.entity.UsageCard
import studio.atopthehill.osom.data.repository.AppRepository
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
class TodayViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: TodayViewModel
    private lateinit var appRepository: AppRepository
    private lateinit var application: Application

    private lateinit var pendingTasksFlow: MutableStateFlow<List<UsageCard>>
    private lateinit var completedTasksFlow: MutableStateFlow<List<UsageCard>>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = mock()
        appRepository = mock()

        pendingTasksFlow = MutableStateFlow(emptyList())
        completedTasksFlow = MutableStateFlow(emptyList())

        whenever(appRepository.getPendingTasks()).thenReturn(pendingTasksFlow)
        whenever(appRepository.getCompletedTasks()).thenReturn(completedTasksFlow)

        viewModel = TodayViewModel(application, appRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should load initial tasks`() = runTest {
        val pending = listOf(UsageCard(1, "Pending", "p", LocalDateTime.now(), "Pending"))
        val completed = listOf(UsageCard(2, "Completed", "c", LocalDateTime.now(), "Completed", status = TaskStatus.COMPLETED))

        val pendingResults = mutableListOf<List<UsageCard>>()
        val pendingJob = launch {
            viewModel.pendingTasks.collect { pendingResults.add(it) }
        }

        val completedResults = mutableListOf<List<UsageCard>>()
        val completedJob = launch {
            viewModel.completedTasks.collect { completedResults.add(it) }
        }

        pendingTasksFlow.value = pending
        completedTasksFlow.value = completed
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(emptyList(), pending), pendingResults)
        assertEquals(listOf(emptyList(), completed), completedResults)

        pendingJob.cancel()
        completedJob.cancel()
    }

    @Test
    fun `onTaskCompleted should move task from pending to completed`() = runTest {
        val task = UsageCard(1, "Test", "com.test", LocalDateTime.now(), "Test Task")
        val results = mutableListOf<List<UsageCard>>()
        val job = launch {
            viewModel.pendingTasks.collect { results.add(it) }
        }

        pendingTasksFlow.value = listOf(task)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onTaskCompleted(task)
        testDispatcher.scheduler.advanceUntilIdle()

        pendingTasksFlow.value = emptyList()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(emptyList(), listOf(task), emptyList()), results)

        job.cancel()
    }

    @Test
    fun `onTaskDismissed should remove task from pending list`() = runTest {
        val task = UsageCard(1, "Test", "com.test", LocalDateTime.now(), "Test Task")
        val results = mutableListOf<List<UsageCard>>()
        val job = launch {
            viewModel.pendingTasks.collect { results.add(it) }
        }

        pendingTasksFlow.value = listOf(task)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onTaskDismissed(task)
        testDispatcher.scheduler.advanceUntilIdle()

        pendingTasksFlow.value = emptyList()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(emptyList(), listOf(task), emptyList()), results)

        job.cancel()
    }

    @Test
    fun `onTaskSnoozed should update timestamp and remove from pending`() = runTest {
        val task = UsageCard(1, "Test", "com.test", LocalDateTime.now(), "Test Task")
        val results = mutableListOf<List<UsageCard>>()
        val job = launch {
            viewModel.pendingTasks.collect { results.add(it) }
        }

        pendingTasksFlow.value = listOf(task)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onTaskSnoozed(task)
        testDispatcher.scheduler.advanceUntilIdle()

        pendingTasksFlow.value = emptyList()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(emptyList(), listOf(task), emptyList()), results)

        job.cancel()
    }

    @Test
    fun `onTaskCompleted should handle repository exception gracefully`() = runTest {
        val task = UsageCard(1, "Test", "com.test", LocalDateTime.now(), "Test Task")
        whenever(appRepository.updateTaskCompletion(eq(task.id), eq(TaskStatus.COMPLETED), any()))
            .doThrow(RuntimeException("Database error"))

        val results = mutableListOf<List<UsageCard>>()
        val job = launch {
            viewModel.pendingTasks.collect { results.add(it) }
        }

        pendingTasksFlow.value = listOf(task)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onTaskCompleted(task)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(emptyList(), listOf(task)), results)

        job.cancel()
    }
}
