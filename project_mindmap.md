Project name: Osom (An android accesibility app to retain context across the mobile device by analysing the screen at all times and managinf relevant infiormation. Meant to reduce congnitive load by remembering things on their behalf and prompting the user at the right time for reminders/follow ups.)
As of date: Aug 5, 2025

## Project Analysis

### 1. Project Overview

Osom is an Android application that functions as a custom launcher and an accessibility tool. Its primary goal is to reduce cognitive load by tracking application usage, analyzing on-screen content, and managing notifications. It appears to be built with modern Android development practices, using Kotlin, Jetpack Compose, and Room.

### 2. Core Components

The project is structured into three main layers: UI, Services, and Data.

*   **UI Layer:** Built with Jetpack Compose. It includes a custom launcher, an app list screen, and a summary screen.
*   **Services Layer:** This is the core of the app's functionality, running in the background to collect data. It includes:
    *   `OsomAccessibilityService`: To read screen content and context.
    *   `OsomNotificationListenerService`: To capture and process notifications.
    *   `AppTimerService`: To track time spent in different apps.
*   **Data Layer:** Manages the application's data using a Room database and a repository pattern.

### 3. Permissions

The application requires a significant number of permissions to function correctly, as defined in `AndroidManifest.xml`:

*   `QUERY_ALL_PACKAGES`: To get a list of all installed applications for the launcher.
*   `KILL_BACKGROUND_PROCESSES`: To manage running applications.
*   `POST_NOTIFICATIONS`: To show its own notifications.
*   `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_DATA_SYNC`: To run background services reliably.
*   `PACKAGE_USAGE_STATS`: To access application usage history.
*   `WRITE_EXTERNAL_STORAGE`: To log information to files.
*   `BIND_ACCESSIBILITY_SERVICE`: To use the accessibility service.
*   `SYSTEM_ALERT_WINDOW`: To draw over other apps (potentially for reminders or prompts).
*   `BIND_NOTIFICATION_LISTENER_SERVICE`: To read notifications.

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
*   **`UsageCard`**: Represents a card for the summary screen, likely showing usage information.
    *   `id`: (Primary Key) Auto-generated ID.
    *   `title`: Title of the card.
    *   `content`: Content of the card.
    *   `timestamp`: When the card was created.
*   **`UserStats`**: Stores statistics about the user's overall phone usage.
    *   `id`: (Primary Key) Auto-generated ID.
    *   `totalScreenTime`: Total screen time.
    *   `unlocks`: Number of device unlocks.
    *   `notificationsReceived`: Number of notifications received.

### 6. UI Layer (Jetpack Compose)

*   **`MainActivity.kt`**: The main entry point of the application. It sets up the Compose content and the navigation. The app is configured to be a home screen launcher (`category.HOME`).
*   **`LauncherScreen.kt` / `LauncherViewModel.kt`**: This is the main screen of the app. The ViewModel (`LauncherViewModel`) is responsible for fetching the list of installed apps from the `AppRepository` and providing them to the UI.
*   **`AppListScreen.kt`**: A screen that likely displays a complete list of all installed applications.
*   **`SummaryScreen.kt`**: A screen to display usage statistics and other relevant information, likely populated by the `UsageCard` entity.
*   **`Screen.kt`**: Defines the navigation routes for the application using a sealed class.

### 7. Services Layer

*   **`OsomAccessibilityService.kt`**: This service has the ability to inspect the content of the screen. It can be used to understand what the user is currently doing on their device, which is central to the app's goal of retaining context.
*   **`OsomNotificationListenerService.kt`**: This service receives all notifications posted by other applications. It can be used to log, analyze, or even dismiss notifications based on certain rules.
*   **`AppTimerService.kt`**: This service likely runs in the foreground to track the amount of time the user spends in each application.

### 8. Data Layer

*   **`AppRepository.kt`**: Follows the repository pattern, acting as a single source of truth for the application's data. It abstracts the data sources (the Room database) from the rest of the app, particularly the ViewModels.
*   **DAOs (`AppInfoDao`, `UsageCardDao`, `UserStatsDao`)**: Data Access Objects for the Room database. They define the SQL queries for interacting with the database tables.
*   **`FileLogger.kt` & `UsageStatsLogger.kt`**: Utility classes for logging information to files, which can be useful for debugging and analysis.

### Summary of Findings

Osom is a complex Android application with a clear purpose. It combines a custom launcher with powerful background services to create a context-aware user experience. The use of modern Android development tools and a well-defined architecture suggests a solid foundation. The core of the application lies in its ability to gather and process large amounts of data about user behavior to provide intelligent assistance.