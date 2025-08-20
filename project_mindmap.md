Project name: Osom (An android accesibility app to retain context across the mobile device by analysing the screen at all times and managinf relevant infiormation. Meant to reduce congnitive load by remembering things on their behalf and prompting the user at the right time for reminders/follow ups.)
As of date: Aug 20, 2025

## Project Analysis

### 1. Project Overview

Osom is an Android application that functions as a custom launcher and an accessibility tool. Its primary goal is to reduce cognitive load by tracking application usage, analyzing on-screen content, and managing notifications. It appears to be built with modern Android development practices, using Kotlin, Jetpack Compose, and Room. The app now features a comprehensive onboarding experience for new users, a Control Center to manage which applications are monitored, and a **Nudge System** to provide timely and context-aware notifications.

### 2. Core Components

The project is structured into three main layers: UI, Services, and Data.

*   **UI Layer:** Built with Jetpack Compose. It includes a custom launcher with an onboarding carousel, a "Today" dashboard for tasks, an app list screen, a summary screen, and a new **Control Center** for managing app monitoring and preferences.
*   **Services Layer:** This is the core of the app's functionality, running in the background to collect data. It includes:
    *   `OsomAccessibilityService`: To read screen content and context from **whitelisted applications**.
    *   `OsomNotificationListenerService`: To capture and process notifications.
    *   `AppTimerService`: To track time spent in different apps.
    *   **`NudgeEngine.kt`**: A rule-based engine responsible for sending active, contextual reminders to the user.
    *   **`NudgeManager.kt`**: A utility class for creating and displaying notifications.
*   **Data Layer:** Manages the application's data using a Room database and a repository pattern. It now includes an `OnboardingManager` to track the completion of the initial user onboarding.

### 3. Permissions

The application uses a **comprehensive 4-step permission wizard** during onboarding to ensure all critical permissions are granted for full functionality:

#### **Multi-Step Permission Onboarding:**
*   **Step 1 - Accessibility Service** (`BIND_ACCESSIBILITY_SERVICE`): To monitor screen content from whitelisted applications for task detection.
*   **Step 2 - Usage Stats Access** (`PACKAGE_USAGE_STATS`): To track app usage time and provide usage insights.
*   **Step 3 - Display Over Other Apps** (`SYSTEM_ALERT_WINDOW`): To show reminders and notifications over other apps when needed.
*   **Step 4 - Show Notifications** (`POST_NOTIFICATIONS`): To send important notifications like app update reminders (Android 13+).

#### **Additional System Permissions:**
*   `QUERY_ALL_PACKAGES`: To get a list of all installed applications for the launcher.
*   `KILL_BACKGROUND_PROCESSES`: To manage running applications.
*   `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_DATA_SYNC`: To run background services reliably.
*   `WRITE_EXTERNAL_STORAGE`: To log information to files.

The enhanced `PermissionScreen.kt` provides a sophisticated wizard with progress tracking, detailed explanations for each permission, and comprehensive status overview to ensure users understand why each permission is needed.

### 4. Dependencies

The project relies on several key libraries from the Android Jetpack and Kotlin ecosystems:

*   **AndroidX:** Core KTX, Lifecycle (ViewModel, LiveData, Runtime), Activity Compose.
*   **Jetpack Compose:** UI, Material3, Graphics, Tooling, Navigation.
*   **Room:** For local database storage (ORM).
*   **Coroutines:** For asynchronous programming.
*   **Coil:** For image loading (used for app icons).

### 5. Database Schema (Room)

The app uses a Room database (`AppDatabase.kt`) with the following entities:

*   **`AppInfo`**: Stores information about installed applications.
    *   `packageName`: (Primary Key) The package name of the app.
    *   `appName`: The user-facing name of the app.
    *   `totalTimeInForeground`: Total time the app has been in the foreground.
    *   `isWhitelisted`: (New) A boolean flag to indicate if the app should be monitored by the accessibility service.
*   **`UsageCard`**: Represents a task or a piece of actionable information for the user.
    *   `id`: (Primary Key) Auto-generated ID.
    *   `appName`: The name of the app this task originated from.
    *   `packageName`: The package name of the source app.
    *   `timestamp`: When the task was created or last updated.
    *   `title`: The main text of the task.
    *   `status`: The current status of the task (e.g., `PENDING`, `COMPLETED`, `DISMISSED`).
    *   `snoozeUntil`: If the task is snoozed, this field holds the time until which it should be hidden.
*   **`UserStats`**: Stores statistics about the user's overall phone usage.
    *   `id`: (Primary Key) Auto-generated ID.
    *   `totalScreenTime`: Total screen time.
    *   `unlocks`: Number of device unlocks.
    *   `notificationsReceived`: Number of notifications received.

### 6. UI Layer (Jetpack Compose)

*   **`MainActivity.kt`**: The main entry point of the application. It sets up the Compose content and the navigation. It now checks for onboarding completion to determine the initial screen.
*   **`LauncherScreen.kt` / `LauncherViewModel.kt`**: This is the main screen of the app. It now includes an **onboarding carousel** for first-time users. The ViewModel (`LauncherViewModel`) is responsible for fetching the list of installed apps from the `AppRepository` and providing them to the UI.
*   **`TodayScreen.kt` / `TodayViewModel.kt`**: This is the main dashboard of the app, showing a list of pending tasks. The ViewModel is responsible for fetching tasks from the `AppRepository` and handling user actions like completing, dismissing, or snoozing a task. It now includes a settings icon to navigate to the Control Center.
*   **`PermissionScreen.kt` / `PermissionViewModel.kt`**: **Enhanced 4-step permission wizard** (395+ lines) that guides users through all required permissions with progress tracking, detailed explanations, and comprehensive status overview. The ViewModel (97+ lines) provides methods to check and manage all critical permissions including accessibility, usage stats, overlay, and notification permissions.
*   **`AppListScreen.kt`**: A screen that likely displays a complete list of all installed applications.
*   **`SummaryScreen.kt`**: A screen to display usage statistics and other relevant information, likely populated by the `UsageCard` entity.
*   **`ControlCenterScreen.kt` / `ControlCenterViewModel.kt`**: (New) This screen allows the user to manage which apps are monitored by the accessibility service by whitelisting them. It also provides a toggle to enable or disable active reminders.
*   **`Screen.kt`**: Defines the navigation routes for the application using a sealed class. It now includes a `ControlCenter` route.

### 7. Services Layer

*   **`OsomAccessibilityService.kt`**: This service has the ability to inspect the content of the screen. It has been updated to **only monitor applications that are whitelisted** by the user in the Control Center. The service can now be dynamically configured to listen to events from specific packages. It is also responsible for **creating tasks and sending silent "New Task Saved" notifications** when a whitelisted app is launched.
*   **`OsomNotificationListenerService.kt`**: This service receives all notifications posted by other applications. It can be used to log, analyze, or even dismiss notifications based on certain rules.
*   **`AppTimerService.kt`**: This service likely runs in the foreground to track the amount of time the user spends in each application.
*   **Nudge System:** A new system designed to provide timely and context-aware notifications.
    *   **`NudgeEngine.kt`**: The brains of the Nudge System. It runs in the background and observes the app's state (e.g., the current foreground app, the list of pending tasks). It contains a set of rules to determine when to send **active, contextual reminders** to the user.
    *   **`NudgeManager.kt`**: A utility class responsible for creating and displaying both silent and active notifications. It handles the construction of the notification UI, including actions like "Mark as Done".
    *   **`NudgeActionReceiver.kt`**: A `BroadcastReceiver` that listens for and handles user actions from notifications, such as marking a task as complete.

### 8. Data Layer

*   **`AppRepository.kt`**: Follows the repository pattern, acting as a single source of truth for the application's data. It abstracts the data sources (the Room database) from the rest of the app, particularly the ViewModels. It now includes a `setWhitelisted` method to update the monitoring status of an app and a `getLatestUsageCardForPackage` method to retrieve the most recent task for a given app.
*   **DAOs (`AppInfoDao`, `UsageCardDao`, `UserStatsDao`)**: Data Access Objects for the Room database. They define the SQL queries for interacting with the database tables. `UsageCardDao` has been updated with queries to manage task status and timestamps, and to fetch the latest task for a package.
*   **`FileLogger.kt` & `UsageStatsLogger.kt`**: Utility classes for logging information to files, which can be useful for debugging and analysis.
*   **`OnboardingManager.kt`**: (New) A utility class that uses `SharedPreferences` to track whether the user has completed the initial onboarding flow.
*   **`PermissionManager.kt`**: Enhanced utility class for managing all app permissions with expanded functionality to handle the 4-step permission wizard flow.

### 9. Testing Framework

The project now includes comprehensive testing infrastructure to ensure reliability and correctness:

#### **Unit Testing:**
*   **`PermissionViewModelTest.kt`**: Extensive testing (276 lines) for all permission management logic using Robolectric for Android-specific testing. Tests all 4 permission types and edge cases.
*   **`PermissionManagerTest.kt`**: Enhanced tests (85+ lines updated) for the expanded permission management functionality.
*   **`NudgeActionReceiverTest.kt`**: Unit tests (61 lines) for nudge action handling using Mockito for mocking dependencies.

#### **Testing Technologies:**
*   **JUnit 4**: Primary testing framework used throughout the project.
*   **Robolectric**: For Android unit tests that need Android context, particularly for permission testing.
*   **Mockito & Mockito-Kotlin**: For mocking dependencies and testing in isolation.
*   **Coroutines Testing**: Uses `kotlinx-coroutines-test` for testing asynchronous operations.

#### **Testing Patterns:**
*   Comprehensive mocking of Android system services (NotificationManager, Context, etc.)
*   Shadowing of system components for controlled testing environments
*   Extensive edge case testing for critical functionality like permissions

### 10. Development Guidance

#### **CLAUDE.md - AI Assistant Integration:**
The project includes comprehensive guidance (214 lines) for AI-assisted development:

*   **Developer Profile**: Tailored for an experienced AI/ML developer (9+ years) who is new to Android development
*   **Communication Guidelines**: Emphasizes clear explanations of mobile concepts with analogies to Python/ML
*   **Development Workflow**: Structured process for code changes with mandatory planning and user confirmation
*   **Code Quality Standards**: Focus on comprehensive comments and Android best practices
*   **Error Handling Strategy**: Systematic approach to debugging and fixing issues

This documentation ensures consistent, educational, and high-quality development practices when working with AI assistance tools.

### Summary of Findings

Osom is a complex Android application with a clear purpose. It combines a custom launcher with powerful background services to create a context-aware user experience. The use of modern Android development tools and a well-defined architecture suggests a solid foundation. The core of the application lies in its ability to gather and process large amounts of data about user behavior to provide intelligent assistance. 

**Recent Major Enhancements:**
*   **Enhanced Permission System**: Comprehensive 4-step permission wizard ensuring all critical permissions are granted during onboarding
*   **Robust Testing Infrastructure**: Extensive unit testing with Robolectric, Mockito, and comprehensive edge case coverage
*   **AI-Assisted Development**: Structured development guidance (CLAUDE.md) tailored for AI/ML developers new to Android
*   **Rule-based Nudge System**: Context-aware notification system with comprehensive action handling
*   **Control Center**: User-controlled app monitoring with whitelisting capabilities

These additions provide users with better control, clearer permission understanding, and a more reliable, well-tested experience. The project demonstrates strong software engineering practices with comprehensive testing and structured development workflows.