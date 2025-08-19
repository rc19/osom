package studio.atopthehill.osom.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import studio.atopthehill.osom.OsomApplication
import studio.atopthehill.osom.config.LogConfig
import studio.atopthehill.osom.data.db.entity.UsageCard
import studio.atopthehill.osom.data.repository.AppRepository
import studio.atopthehill.osom.utils.FileLogger
import studio.atopthehill.osom.utils.managers.NudgeManager
import java.time.LocalDateTime

class OsomAccessibilityService : AccessibilityService() {

    private val TAG = "OsomAccessibilityService"
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var lastActivePackage: String? = null
    private lateinit var appRepository: AppRepository
    private lateinit var nudgeManager: NudgeManager

    companion object {
        const val ACTION_SET_ACCESSIBILITY_TARGET =
                "studio.atopthehill.osom.services.action.SET_ACCESSIBILITY_TARGET"
        const val ACTION_CLEAR_ACCESSIBILITY_TARGET =
                "studio.atopthehill.osom.services.action.CLEAR_ACCESSIBILITY_TARGET"
        const val EXTRA_PACKAGE_NAME = "studio.atopthehill.osom.services.extra.PACKAGE_NAME"
    }

    private val accessibilityTargetReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        ACTION_SET_ACCESSIBILITY_TARGET -> {
                            val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                            if (!packageName.isNullOrEmpty()) {
                                Log.d(TAG, "Setting accessibility target to: $packageName")
                                updateServiceInfo(arrayOf(packageName))
                            } else {
                                Log.w(
                                        TAG,
                                        "Received SET_ACCESSIBILITY_TARGET with null or empty package name."
                                )
                                updateServiceInfo(emptyArray()) // Default to no specific target
                            }
                        }
                        ACTION_CLEAR_ACCESSIBILITY_TARGET -> {
                            Log.d(TAG, "Clearing accessibility target.")
                            updateServiceInfo(
                                    emptyArray()
                            ) // Set to empty to stop listening to specifics
                        }
                    }
                }
            }

    override fun onCreate() {
        super.onCreate()
        appRepository = (application as OsomApplication).appRepository
        nudgeManager = NudgeManager(this)
        val intentFilter =
                IntentFilter().apply {
                    addAction(ACTION_SET_ACCESSIBILITY_TARGET)
                    addAction(ACTION_CLEAR_ACCESSIBILITY_TARGET)
                }
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(accessibilityTargetReceiver, intentFilter)
        Log.d(TAG, "Accessibility Service onCreate, receiver registered.")
    }

    private fun updateServiceInfo(targetPackageNames: Array<String>?) {
        // It's crucial to get the current service info if available and modify it,
        // or re-create it ensuring all necessary flags and configurations are preserved.
        val currentInfo = this.serviceInfo // Get the currently active service info
        val newInfo = AccessibilityServiceInfo()

        // Copy essential properties from currentInfo if available, or set defaults
        if (currentInfo != null) {
            try {
                newInfo.eventTypes = currentInfo.eventTypes
                newInfo.feedbackType = currentInfo.feedbackType
                newInfo.flags = currentInfo.flags
                newInfo.notificationTimeout = currentInfo.notificationTimeout
                // newInfo.description = currentInfo.description // description is from metadata
                // usually
                // Only copy capabilities if they are managed programmatically, typically they are
                // from XML.
                // newInfo.setCapabilities(currentInfo.capabilities)
            } catch (e: Exception) {
                Log.e(TAG, "Error copying current service info, re-initializing.", e)
                initializeServiceInfoObject(newInfo) // Fallback to re-initialization
            }
        } else {
            initializeServiceInfoObject(newInfo)
        }

        newInfo.packageNames = targetPackageNames

        this.serviceInfo = newInfo
        Log.d(
                TAG,
                "ServiceInfo updated. Target packages: ${targetPackageNames?.joinToString() ?: "NONE"}"
        )
        // Ensure the system picks up the changes
        setServiceInfo(newInfo)
    }

    private fun initializeServiceInfoObject(info: AccessibilityServiceInfo) {
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK

        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL
        // Flags should not include capabilities. Capabilities are typically set via XML or
        // dedicated capability APIs.
        // FLAG_REPORT_VIEW_IDS is useful for getting resource names.
        // FLAG_RETRIEVE_INTERACTIVE_WINDOWS allows interaction with windows.
        info.flags =
                AccessibilityServiceInfo.DEFAULT or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                        AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        // info.flags = info.flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS //
        // If you need all views

        info.notificationTimeout = 100
        // Note: Capabilities like canTakeScreenshot, canRetrieveWindowContent are primarily set via
        // XML.
        // AndroidManifest.xml references accessibility_service_config.xml where these are set.
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        nudgeManager = NudgeManager(this)
        Log.d(TAG, "onServiceConnected: Accessibility Service connected.")
        // Initial configuration: listen to no specific package until a target is set.
        val info = AccessibilityServiceInfo()
        initializeServiceInfoObject(info) // Set default flags, event types etc.
        info.packageNames = null // Initially, don't target any specific package; listen to all.

        // Apply the initial configuration
        setServiceInfo(info)
        Log.d(TAG, "Service Info configured initially. Target packages: ALL (null)")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        val eventType = event.eventType

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (packageName != lastActivePackage) {
                (application as OsomApplication).foregroundApp.value = packageName

                serviceScope.launch {
                    val whitelistedApps = appRepository.allInstalledApps.first().filter { it.isWhitelisted }
                    val isWhitelisted = whitelistedApps.any { it.packageName == packageName }

                    if (isWhitelisted) {
                        val appInfo = appRepository.getAppByPackageName(packageName)
                        val appName = appInfo?.label ?: packageName
                        val taskTitle = "Opened $appName"

                        val usageCard = UsageCard(
                            appName = appName,
                            packageName = packageName,
                            timestamp = LocalDateTime.now(),
                            title = taskTitle
                        )
                        appRepository.insertUsageCard(usageCard)
                        nudgeManager.showSilentNotification("Osom saved a new task: '$taskTitle'")
                    }
                }
                lastActivePackage = packageName
            }
        }

        val currentServiceInfo = serviceInfo

        serviceScope.launch {
            val whitelistedApps = appRepository.allInstalledApps.first().filter { it.isWhitelisted }
            val isWhitelisted = whitelistedApps.any { it.packageName == packageName }

            if (!isWhitelisted) {
                return@launch
            }

            // TODO: Process events to gather insights
            Log.d(TAG, "Event from whitelisted app received: $packageName")

            if (LogConfig.logAccessibilityEvents) {
                val currentPackageFilters =
                    currentServiceInfo?.packageNames?.joinToString(", ") ?: "ALL (null)"
                val eventTypeString = AccessibilityEvent.eventTypeToString(event.eventType)
                val className = event.className?.toString() ?: "N/A"
                // val eventText = event.text?.joinToString(", ") ?: "N/A" // We will log more detailed
                // text below

                Log.d(
                    TAG,
                    "AccessibilityEvent: Pkg[$packageName], Type[$eventTypeString], Class[$className], CurrentFilters[$currentPackageFilters]"
                )
                FileLogger.log(
                    TAG,
                    "AccessibilityEvent: Pkg[$packageName], Type[$eventTypeString], Class[$className], CurrentFilters[$currentPackageFilters]"
                )

                // Log detailed window content if canRetrieveWindowContent is enabled
                if (currentServiceInfo?.canRetrieveWindowContent == true) {
                    val sourceNode = event.source
                    if (sourceNode != null) {
                        val sourceNodeLog =
                            "Event Source Node: ${sourceNode.className}, Text: '${sourceNode.text}', ContentDesc: '${sourceNode.contentDescription}', ViewId: ${sourceNode.viewIdResourceName}"
                        Log.d(TAG, sourceNodeLog)
                        FileLogger.log(TAG, sourceNodeLog)
                        logNodeHierarchy(sourceNode, 0, true)
                        sourceNode.recycle() // Important to recycle nodes
                    } else {
                        val noSourceLog =
                            "Event source node is null for Pkg[$packageName], Type[$eventTypeString]"
                        Log.d(TAG, noSourceLog)
                        FileLogger.log(TAG, noSourceLog)
                    }
                }

                when (event.eventType) {
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                        // Log raw integer for contentChangeTypes, toString helper not directly
                        // available
                        val log =
                            "Window State Changed: Pkg[$packageName], ContentChangeTypesRaw: ${event.contentChangeTypes}, Action: ${event.action}, EventText: ${event.text.joinToString()}, WindowId: ${event.windowId}"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                        val log =
                            "View Clicked: Pkg[$packageName] (see node hierarchy for details of source: ${event.source?.viewIdResourceName ?: "N/A"})"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                        val log =
                            "Notification State Changed: Pkg[$packageName], EventText: ${event.text.joinToString()}, ParcelableData: ${event.parcelableData}"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_ANNOUNCEMENT -> {
                        val log =
                            "Announcement: Pkg[$packageName], EventText: ${event.text.joinToString()}"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_ASSIST_READING_CONTEXT -> {
                        // This event is usually requested by the system for assist purposes.
                        // The main data is in the source node's hierarchy.
                        val log =
                            "Assist Reading Context: Pkg[$packageName] (see node hierarchy for details)"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED -> { // Corrected constant name
                        val log =
                            "Context Clicked: Pkg[$packageName] (see node hierarchy for details of source: ${event.source?.viewIdResourceName ?: "N/A"})"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_GESTURE_DETECTION_START -> {
                        val log = "Gesture Detection Started: Pkg[$packageName]"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_GESTURE_DETECTION_END -> {
                        val log = "Gesture Detection Ended: Pkg[$packageName]"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START -> {
                        val log = "Touch Exploration Gesture Started: Pkg[$packageName]"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END -> {
                        val log = "Touch Exploration Gesture Ended: Pkg[$packageName]"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> {
                        val log = "Touch Interaction Started: Pkg[$packageName]"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> {
                        val log = "Touch Interaction Ended: Pkg[$packageName]"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> {
                        val log =
                            "View Accessibility Focused: Pkg[$packageName] (see node hierarchy for details of source: ${event.source?.viewIdResourceName ?: "N/A"})"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED -> {
                        val log =
                            "View Accessibility Focus Cleared: Pkg[$packageName] (see node hierarchy for details of source: ${event.source?.viewIdResourceName ?: "N/A"})"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                        val log =
                            "View Focused: Pkg[$packageName] (see node hierarchy for details of source: ${event.source?.viewIdResourceName ?: "N/A"})"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_VIEW_HOVER_ENTER -> {
                        val log =
                            "View Hover Enter: Pkg[$packageName] (see node hierarchy for details of source: ${event.source?.viewIdResourceName ?: "N/A"})"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_VIEW_HOVER_EXIT -> {
                        val log =
                            "View Hover Exit: Pkg[$packageName] (see node hierarchy for details of source: ${event.source?.viewIdResourceName ?: "N/A"})"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
                        val log =
                            "View Long Clicked: Pkg[$packageName] (see node hierarchy for details of source: ${event.source?.viewIdResourceName ?: "N/A"})"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                        var scrollDetails =
                            "ScrollX: ${event.scrollX}, ScrollY: ${event.scrollY}, MaxScrollX: ${event.maxScrollX}, MaxScrollY: ${event.maxScrollY}"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            scrollDetails +=
                                ", ScrollDeltaX: ${event.scrollDeltaX}, ScrollDeltaY: ${event.scrollDeltaY}"
                        }
                        val log =
                            "View Scrolled: Pkg[$packageName], $scrollDetails (see node hierarchy for details of source: ${event.source?.viewIdResourceName ?: "N/A"})"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_VIEW_SELECTED -> {
                        val log =
                            "View Selected: Pkg[$packageName], EventText: ${event.text.joinToString()} (see node hierarchy for details of source: ${event.source?.viewIdResourceName ?: "N/A"})"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                        val log =
                            "View Text Changed: Pkg[$packageName], FromIndex: ${event.fromIndex}, Added: ${event.addedCount}, Removed: ${event.removedCount}, BeforeText: '${event.beforeText}', EventText: ${event.text.joinToString()} (see node for current text)"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                        val log =
                            "View Text Selection Changed: Pkg[$packageName], FromIndex: ${event.fromIndex}, ToIndex: ${event.toIndex}, ItemCount: ${event.itemCount}, EventText: ${event.text.joinToString()} (current selection)"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY -> {
                        // Log raw integers for movementGranularity and action
                        val log =
                            "View Text Traversed: Pkg[$packageName], GranularityRaw: ${event.movementGranularity}, ActionRaw: ${event.action}, FromIndex: ${event.fromIndex}, ToIndex: ${event.toIndex}, EventText: ${event.text.joinToString()}"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                        // Log raw integer for contentChangeTypes
                        val log =
                            "Window Content Changed: Pkg[$packageName], ContentChangeTypesRaw: ${event.contentChangeTypes}, WindowId: ${event.windowId} (see node hierarchy for details if source exists: ${event.source?.viewIdResourceName ?: "N/A"})"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                        var windowChangesStr = ""
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            // Log raw integer for windowChanges
                            windowChangesStr = " WindowChangesRaw: ${event.windowChanges}"
                        }
                        val log =
                            "Windows Changed: Pkg[$packageName], EventTime: ${event.eventTime}$windowChangesStr"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                    // Default case for any other event types not explicitly handled above
                    else -> {
                        val log =
                            "Other Accessibility Event: Pkg[$packageName], EventType: ${AccessibilityEvent.eventTypeToString(event.eventType)} (see node hierarchy if source exists)"
                        Log.d(TAG, log)
                        FileLogger.log(TAG, log)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "onInterrupt: Accessibility Service interrupted.")
        FileLogger.log(TAG, "onInterrupt: Accessibility Service interrupted.")
    }

    // Helper function to recursively log node hierarchy
    private fun logNodeHierarchy(nodeInfo: AccessibilityNodeInfo?, depth: Int, logToFile: Boolean) {
        if (nodeInfo == null) return

        val indent = "  ".repeat(depth)
        val logMessage = StringBuilder()

        logMessage.append("$indent[${nodeInfo.className ?: "N/A"}]\n")
        logMessage.append("$indent  Package: ${nodeInfo.packageName ?: "N/A"}\n")
        logMessage.append("$indent  View ID: ${nodeInfo.viewIdResourceName ?: "N/A"}\n")
        logMessage.append("$indent  Window ID: ${nodeInfo.windowId}\n")

        val text = nodeInfo.text
        if (text != null) {
            logMessage.append("$indent  Text: $text\n")
            logMessage.append(
                    "$indent    Selection: Start=${nodeInfo.textSelectionStart}, End=${nodeInfo.textSelectionEnd}\n"
            )
            logMessage.append("$indent    MaxTextLength: ${nodeInfo.maxTextLength}\n")
            logMessage.append("$indent    ShowingHintText: ${nodeInfo.isShowingHintText}\n")
            val hint = nodeInfo.hintText
            if (hint != null) {
                logMessage.append("$indent    HintText: $hint\n")
            }
        }
        val contentDesc = nodeInfo.contentDescription
        if (contentDesc != null) {
            logMessage.append("$indent  ContentDesc: $contentDesc\n")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val tooltip = nodeInfo.tooltipText
            if (tooltip != null) {
                logMessage.append("$indent  Tooltip: $tooltip\n")
            }
        }
        val error = nodeInfo.error
        if (error != null) {
            logMessage.append("$indent  Error: $error\n")
        }

        val boundsInParent = Rect()
        nodeInfo.getBoundsInParent(boundsInParent)
        logMessage.append("$indent  BoundsInParent: $boundsInParent\n")
        val boundsInScreen = Rect()
        nodeInfo.getBoundsInScreen(boundsInScreen)
        logMessage.append("$indent  BoundsInScreen: $boundsInScreen\n")

        logMessage.append(
                "$indent  State: Enabled=${nodeInfo.isEnabled}, Visible=${nodeInfo.isVisibleToUser}, Focused=${nodeInfo.isFocused}, AccFocused=${nodeInfo.isAccessibilityFocused}\n"
        )
        logMessage.append(
                "$indent  Behavior: Clickable=${nodeInfo.isClickable}, LongClickable=${nodeInfo.isLongClickable}, ContextClickable=${nodeInfo.isContextClickable}\n"
        )
        logMessage.append(
                "$indent  ContentProps: Checkable=${nodeInfo.isCheckable}, Checked=${nodeInfo.isChecked}, Password=${nodeInfo.isPassword}, Scrollable=${nodeInfo.isScrollable}\n"
        )
        logMessage.append(
                "$indent  InputProps: Editable=${nodeInfo.isEditable}, InputType=${nodeInfo.inputType}, MultiLine=${nodeInfo.isMultiLine}\n"
        )
        logMessage.append(
                "$indent  OtherProps: ContentInvalid=${nodeInfo.isContentInvalid}, Dismissable=${nodeInfo.isDismissable}, CanOpenPopup=${nodeInfo.canOpenPopup()}\n"
        )
        logMessage.append("$indent  DrawingOrder: ${nodeInfo.drawingOrder}\n")

        logMessage.append(
                "$indent  Actions: ${nodeInfo.actionList?.joinToString { action -> action.label ?: action.id.toString() } ?: "N/A"}\n"
        )

        nodeInfo.collectionInfo?.let {
            logMessage.append(
                    "$indent  CollectionInfo: Rows=${it.rowCount}, Cols=${it.columnCount}, Hierarchical=${it.isHierarchical}, SelectionMode=${it.selectionMode}\n"
            )
        }
        nodeInfo.collectionItemInfo?.let {
            logMessage.append(
                    "$indent  CollectionItemInfo: Row=${it.rowIndex}, Col=${it.columnIndex}, RowSpan=${it.rowSpan}, ColSpan=${it.columnSpan}, Heading=${it.isHeading}, Selected=${it.isSelected}\n"
            )
        }
        nodeInfo.rangeInfo?.let {
            logMessage.append(
                    "$indent  RangeInfo: Type=${it.type}, Min=${it.min}, Max=${it.max}, Current=${it.current}\n"
            )
        }

        logMessage.append("$indent  ChildCount: ${nodeInfo.childCount}")

        val finalLogString = logMessage.toString()
        Log.d(TAG, finalLogString)
        if (logToFile) FileLogger.log(TAG, finalLogString)

        for (i in 0 until nodeInfo.childCount) {
            val childNode = nodeInfo.getChild(i)
            logNodeHierarchy(childNode, depth + 1, logToFile)
            childNode?.recycle() // Recycle child node after use
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(accessibilityTargetReceiver)
        Log.d(TAG, "Accessibility Service onDestroy, receiver unregistered.")
        FileLogger.log(TAG, "Accessibility Service onDestroy, receiver unregistered.")
    }
}
