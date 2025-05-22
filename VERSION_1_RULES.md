# OSOM Launcher - Version 1 Requirements

## Core Features

### 1. Chat Interface
- **Location**: Primary interface on homescreen
- **Behavior**:
  - Empty text bar always visible
  - Clears on:
    - Returning to homescreen
    - Phone unlock
    - App launch
  - No auto-correct
  - No auto-suggest
  - No auto-complete
  - Single line input

### 2. App Database Architecture
- **Components**:
  - Entity: `AppInfo`
    ```kotlin
    data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: ByteArray,
        val isInstalled: Boolean,
        val lastUpdated: Long
    )
    ```
  - Entity: `AppUsage`
    ```kotlin
    data class AppUsage(
        val packageName: String,
        val timestamp: Long,
        val reason: String
    )
    ```
  - DAO: `AppInfoDao` and `AppUsageDao`
  - Repository: `AppRepository`
  - ViewModel: `LauncherViewModel`

- **Database Operations**:
  - Initial fetch on first launch
  - Refresh on app not found
  - Mark apps as uninstalled (don't delete)
  - Store app icons (unused in v1 but stored)
  - Track app usage with reasons

### 3. App Matching Logic
- **Algorithm**:
  1. Split user input into words
  2. For each app in database:
     - Split app label into words
     - Count matching words
     - Store match count
  3. Select app with highest word match count
  4. If multiple apps have same count, use first match

- **Example**:
  ```
  User Input: "youtube music"
  Matches:
  - "YouTube" -> 1 match
  - "YouTube Music" -> 2 matches (selected)
  ```

### 4. App Launch Flow
1. User types app name
2. System searches database using matching logic
3. If app found:
   - Ask user for reason
   - Store usage in database
   - Launch app
4. If app not found:
   - Refresh app database
   - Retry search
   - If still not found, show "App not found" message

## Technical Requirements

### Architecture
- Follow MVVM pattern
- Use Room for database
- Use Kotlin Coroutines for async operations
- Use Jetpack Compose for UI
- Use ViewModel for state management
- Use Repository pattern for data operations

### Database Schema
```sql
-- AppInfo Table
CREATE TABLE app_info (
    package_name TEXT PRIMARY KEY,
    label TEXT NOT NULL,
    icon BLOB,
    is_installed BOOLEAN DEFAULT TRUE,
    last_updated INTEGER
);

-- AppUsage Table
CREATE TABLE app_usage (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    package_name TEXT,
    timestamp INTEGER,
    reason TEXT,
    FOREIGN KEY(package_name) REFERENCES app_info(package_name)
);
```

### Performance Requirements
- App database refresh should not block UI
- App matching should be fast (< 100ms)
- Database operations should be on background thread
- UI should remain responsive during all operations

### Error Handling
- Handle database errors gracefully
- Handle app launch failures
- Handle permission issues
- Log all errors for debugging

### Testing Requirements
- Unit tests for:
  - App matching logic
  - Database operations
  - Repository methods
- UI tests for:
  - Chat interface
  - App launch flow
- Integration tests for:
  - Database operations
  - App launch process

## Version 1 Limitations
- No app icons in UI
- No app suggestions
- No app categories
- No app search history
- No app favorites
- No custom themes
- No settings screen

## Future Considerations (Not for v1)
- App icon display
- Search history
- App categories
- Custom themes
- Settings screen
- App suggestions
- App favorites
- Multiple word matching improvements
- Fuzzy matching
- Auto-complete
- Auto-suggest 