# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Developer Background & Communication Style

The primary developer has:
- 9+ years of experience in AI/ML with Python expertise
- Masters in Computer Science with AI specialization
- **Zero mobile app development experience** (new to Kotlin, Android development)

**Communication Guidelines:**
- Explain mobile development concepts clearly, don't assume Android knowledge
- Always explain the "why" behind technical decisions
- Use analogies to Python/ML concepts when helpful
- Provide educational context for Android-specific patterns

## Development Workflow & Code Changes

**CRITICAL: Always Follow This Process for Code Changes:**

1. **Before Any Code Changes:**
   - Reference `project_mindmap.md` for current codebase context
   - For bugs: First explain the root cause in simple terms
   - Create a detailed implementation plan with alternatives
   - Explain why you prefer one approach over others
   - Get user confirmation before proceeding

2. **When Planning Changes:**
   - Think through multiple approaches and trade-offs
   - Explain the user journey/experience impact
   - Consider all edge cases for UI changes
   - Never suggest changes without explanation first

3. **Error Handling Strategy:**
   - Analyze and group similar errors together
   - Solve each error type systematically, one by one
   - Don't try to fix all errors at once

4. **Code Quality Standards:**
   - Write comprehensive comments for the mobile-dev newcomer
   - Don't use comments to communicate with user
   - Use Android best practices and explain choices
   - Test for syntax/compile errors before finalizing

**Build/Run/Test Policy:**
- Don't use shell commands - user builds in Android Studio
- Provide detailed testing instructions for Android Studio
- User will provide feedback on build/run results

### Testing Framework
- Uses JUnit 4 for unit tests
- Robolectric for Android unit tests that need Android context
- Mockito and Mockito-Kotlin for mocking
- Espresso and UI Automator for instrumented tests
- AndroidX Test for lifecycle-aware testing

## Project Overview

Osom is a sophisticated Android accessibility application built with modern Android development practices. It acts as a "Proactive Contextual Assistant" designed to reduce cognitive load by observing user activity, understanding commitments, and intelligently reminding users at relevant moments. The app uses Android's Accessibility Service to read screen content from whitelisted applications and provides context-aware nudges.


### Testing Framework
- Uses JUnit 4 for unit tests
- Robolectric for Android unit tests that need Android context
- Mockito and Mockito-Kotlin for mocking
- Espresso and UI Automator for instrumented tests
- AndroidX Test for lifecycle-aware testing

## Architecture & Key Components

### Core Architecture Layers

**UI Layer (Jetpack Compose)**
- `MainActivity.kt`: Entry point with navigation setup
- `LauncherScreen.kt`: Custom launcher with onboarding carousel
- `TodayScreen.kt`: Main dashboard showing pending tasks
- `ControlCenterScreen.kt`: App whitelist management and settings
- `Screen.kt`: Navigation routes using sealed classes

**Services Layer (Background Processing)**
- `OsomAccessibilityService.kt`: Reads screen content from whitelisted apps only
- `OsomNotificationListenerService.kt`: Captures and processes notifications
- `AppTimerService.kt`: Tracks app usage time
- `NudgeEngine.kt`: Rule-based engine for contextual reminders
- `NudgeManager.kt`: Handles notification creation and display
- `NudgeActionReceiver.kt`: Processes user actions from notifications

**Data Layer (Room + Repository Pattern)**
- `AppDatabase.kt`: Room database with migration support (version 8)
- `AppRepository.kt`: Single source of truth following repository pattern
- Database entities: `AppInfo`, `UsageCard`, `UserStats`

### Database Schema

**AppInfo Table:**
- Stores installed app information with `isWhitelisted` flag for privacy control
- Package name as primary key, includes foreground time tracking

**UsageCard Table:**
- Represents tasks/actionable items with status management
- Includes snooze functionality and timestamp tracking
- Links to source app via package name

**UserStats Table:**
- Aggregates usage statistics (screen time, unlocks, notifications)
- Includes `activeReminders` field (added in migration 7→8)

### Key Features

**Privacy-First Architecture:**
- User-controlled app whitelisting via Control Center
- Only monitors explicitly approved applications
- Granular permission management

**Nudge System:**
- Context-aware notifications based on app state
- Rule-based engine for intelligent timing
- Silent and active notification types

**Onboarding System:**
- `OnboardingManager.kt` tracks completion via SharedPreferences
- Carousel-based first-time user experience

## Important Implementation Notes

### Accessibility Service Configuration
- Service dynamically updates target packages based on whitelist
- Comprehensive event logging with hierarchical node inspection
- Proper node recycling for memory management
- Detailed logging controlled by `LogConfig.logAccessibilityEvents`

### Coroutines & Threading
- Uses `SupervisorJob` for application-level coroutine scope
- Services use their own coroutine scopes with proper lifecycle management
- Repository operations use `Dispatchers.IO` for database access

### Testing Patterns
- ViewModels tested with `ArchTaskExecutor` for LiveData
- Coroutine testing with `kotlinx-coroutines-test`
- Accessibility service testing with Robolectric
- UI testing with Compose UI test framework

### Dependencies Management
- Uses Gradle Version Catalog (`libs.versions.toml`)
- KSP for Room annotation processing
- Material3 and extended icons for UI
- Coil for app icon loading

## Common Development Workflows

### Adding New Database Entities
1. Create entity class in `data/db/entity/`
2. Create corresponding DAO in `data/db/dao/`
3. Update `AppDatabase.kt` with new entity and DAO
4. Increment database version and add migration if needed
5. Update `AppRepository.kt` with new data access methods

### Creating New UI Screens
1. Follow existing pattern: Screen + ViewModel + State management
2. Add route to `Screen.kt` sealed class
3. Use Compose navigation with proper state hoisting
4. Implement proper lifecycle-aware ViewModels

### Extending Nudge System
1. Add rules to `NudgeEngine.kt` for new trigger conditions
2. Update `NudgeManager.kt` for new notification types
3. Handle new actions in `NudgeActionReceiver.kt`
4. Test with both unit and integration tests

### Accessibility Service Changes
- Always test with multiple target apps
- Ensure proper node recycling to prevent memory leaks
- Respect user privacy settings and whitelist
- Log extensively for debugging but consider performance impact

## Critical Project Files & Usage

### Essential Reference Files (Always Check These First)

**`project_mindmap.md`** - **ALWAYS reference before any work**
- Latest comprehensive documentation about the project
- Designed for quick developer onboarding
- Contains: project overview, technologies, entry points, folder structure, build/test/run instructions, architecture patterns
- **Important:** When asked to update this file, present a high-level overview first, wait for user confirmation, then proceed

**`project_vision.md`** - Ultimate project goals and roadmap
- Defines what the project should achieve and how to get there
- Use this to understand long-term objectives when making decisions

### Development Resources

**`potential_development_ideas/`** - Feature specifications and development roadmap
- Contains detailed feature requirements and bug lists
- Reference when planning new features or fixing known issues

**`gradle/libs.versions.toml`** - Dependency versions and library management
- Central location for all dependency versions
- Always use this for adding new dependencies

## External API & Library Usage

**Important:** When working with external libraries or Android APIs:
- Always perform web searches to check current usage patterns
- Don't rely on implicit knowledge of API usage
- Verify syntax and patterns with official documentation
- Android APIs change frequently, so always verify current practices

## Project Files to Reference

- `project_vision.md`: Comprehensive vision and implementation strategy
- `project_mindmap.md`: Current architecture analysis and component overview
- `potential_development_ideas/`: Feature specifications and development roadmap
- `gradle/libs.versions.toml`: Dependency versions and library management