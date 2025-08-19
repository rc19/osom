package studio.atopthehill.osom.services

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLog
import studio.atopthehill.osom.OsomApplication
import studio.atopthehill.osom.config.LogConfig
import studio.atopthehill.osom.data.db.entity.AppInfo
import studio.atopthehill.osom.data.repository.AppRepository
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class OsomAccessibilityServiceTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var service: OsomAccessibilityService
    private lateinit var appRepository: AppRepository
    private val installedAppsFlow = MutableStateFlow<List<AppInfo>>(emptyList())

    private val whitelistedApp = AppInfo(packageName = "com.whitelisted.app", label = "Whitelisted App", icon = null, isWhitelisted = true)
    private val nonWhitelistedApp = AppInfo(packageName = "com.nonwhitelisted.app", label = "Non-whitelisted App", icon = null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        ShadowLog.stream = System.out // Redirect Logcat to System.out to verify logs
        appRepository = mock()
        whenever(appRepository.allInstalledApps).thenReturn(installedAppsFlow)

        service = Robolectric.setupService(OsomAccessibilityService::class.java)
        val application = service.application as OsomApplication
        application.appRepository = appRepository // Inject mock repository before onCreate
        service.onCreate()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `no whitelisted apps - should not log events`() = runTest {
        installedAppsFlow.value = listOf(nonWhitelistedApp)

        val event = AccessibilityEvent.obtain()
        event.packageName = nonWhitelistedApp.packageName
        event.eventType = AccessibilityEvent.TYPE_VIEW_CLICKED

        service.onAccessibilityEvent(event)
        testDispatcher.scheduler.advanceUntilIdle()

        val logs = ShadowLog.getLogs()
        assertFalse(logs.any { it.msg.contains("Event from whitelisted app received") })
    }

    @Test
    fun `one whitelisted app - should only log events from that app`() = runTest {
        installedAppsFlow.value = listOf(whitelistedApp, nonWhitelistedApp)

        // Event from whitelisted app
        val whitelistedEvent = AccessibilityEvent.obtain()
        whitelistedEvent.packageName = whitelistedApp.packageName
        whitelistedEvent.eventType = AccessibilityEvent.TYPE_VIEW_CLICKED

        service.onAccessibilityEvent(whitelistedEvent)
        testDispatcher.scheduler.advanceUntilIdle()

        var logs = ShadowLog.getLogs()
        assertTrue(logs.any { it.msg.contains("Event from whitelisted app received: ${whitelistedApp.packageName}") })

        ShadowLog.clear()

        // Event from non-whitelisted app
        val nonWhitelistedEvent = AccessibilityEvent.obtain()
        nonWhitelistedEvent.packageName = nonWhitelistedApp.packageName
        nonWhitelistedEvent.eventType = AccessibilityEvent.TYPE_VIEW_CLICKED

        service.onAccessibilityEvent(nonWhitelistedEvent)
        testDispatcher.scheduler.advanceUntilIdle()

        logs = ShadowLog.getLogs()
        assertFalse(logs.any { it.msg.contains("Event from whitelisted app received") })
    }
}
