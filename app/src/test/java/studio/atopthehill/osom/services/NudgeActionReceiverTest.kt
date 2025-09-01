package studio.atopthehill.osom.services

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import studio.atopthehill.osom.OsomApplication
import studio.atopthehill.osom.data.db.entity.TaskStatus
import studio.atopthehill.osom.data.repository.AppRepository

@ExperimentalCoroutinesApi
class NudgeActionReceiverTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var application: OsomApplication

    @Mock
    private lateinit var intent: Intent

    @Mock
    private lateinit var appRepository: AppRepository

    @Mock
    private lateinit var notificationManager: NotificationManager

    private lateinit var nudgeActionReceiver: NudgeActionReceiver

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        nudgeActionReceiver = NudgeActionReceiver()
        `when`(context.applicationContext).thenReturn(application)
        `when`(application.appRepository).thenReturn(appRepository)
        `when`(context.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(notificationManager)
    }

    @Test
    fun `test mark as done action`() = runTest {
        // Given
        val taskId = 123L
        `when`(intent.action).thenReturn(NudgeActionReceiver.ACTION_MARK_AS_DONE)
        `when`(intent.getLongExtra(NudgeActionReceiver.EXTRA_TASK_ID, -1)).thenReturn(taskId)

        // When
        nudgeActionReceiver.onReceive(context, intent)
        
        // Wait for coroutine to complete (the receiver uses CoroutineScope(Dispatchers.IO))
        delay(100) // Small delay to allow async operations to complete

        // Then
        verify(appRepository).updateTaskStatus(taskId, TaskStatus.COMPLETED)
        verify(notificationManager).cancel(taskId.toInt())
    }
}
