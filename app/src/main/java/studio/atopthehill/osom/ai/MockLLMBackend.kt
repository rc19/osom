package studio.atopthehill.osom.ai

import android.util.Log
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Mock implementation of SimpleLLMBackend for testing and MVP development.
 * 
 * This backend simulates realistic AI behavior without requiring actual model inference.
 * It provides:
 * - Realistic enhanced task descriptions based on common app patterns
 * - Simulated processing delays to mimic real AI inference
 * - Occasional failures to test fallback logic
 * - App-specific enhancement patterns for better testing
 */
class MockLLMBackend : SimpleLLMBackend {
    
    companion object {
        private const val TAG = "MockLLMBackend"
        private const val FAILURE_RATE = 0.1 // 10% failure rate for testing fallback
        private const val MIN_DELAY_MS = 300L
        private const val MAX_DELAY_MS = 1500L
    }
    
    private var isInitialized = false
    
    /**
     * Predefined enhancement patterns for common apps to provide realistic testing.
     * In a real implementation, this would be replaced by actual AI model inference.
     */
    private val enhancementPatterns = mapOf(
        "com.google.android.gm" to listOf(
            "Reply to email about meeting agenda",
            "Check new message from colleague",
            "Draft email response to client inquiry",
            "Review important notification from boss",
            "Compose email for project update"
        ),
        "com.google.android.calendar" to listOf(
            "Add dentist appointment to schedule",
            "Review upcoming meeting details",
            "Schedule team standup for next week",
            "Check conflict for lunch meeting",
            "Set reminder for project deadline"
        ),
        "com.google.android.apps.messaging" to listOf(
            "Respond to friend's dinner invitation",
            "Check family group chat updates",
            "Reply to work-related text message",
            "Send location details to meetup group",
            "Confirm weekend plans with friends"
        ),
        "com.whatsapp" to listOf(
            "Reply to group chat discussion",
            "Check voice message from mom",
            "Share project update with team",
            "Respond to client's urgent message",
            "Send birthday wishes to friend"
        ),
        "com.android.chrome" to listOf(
            "Continue reading JavaScript tutorial",
            "Research vacation destinations",
            "Check flight booking confirmation",
            "Review online shopping cart items",
            "Read article about Android development"
        ),
        "com.spotify.music" to listOf(
            "Create playlist for morning workout",
            "Discover new podcast episodes",
            "Search for relaxing study music",
            "Save song heard on radio",
            "Check out friend's music recommendations"
        ),
        "com.instagram.android" to listOf(
            "Respond to friend's story",
            "Check new posts from followed accounts",
            "Share photo from today's adventure",
            "Browse trending reels",
            "Reply to direct message"
        )
    )
    
    override suspend fun initialize(): Boolean {
        Log.d(TAG, "Initializing MockLLMBackend...")
        
        // Simulate initialization delay
        delay(Random.nextLong(500, 1000))
        
        isInitialized = true
        Log.d(TAG, "MockLLMBackend initialized successfully")
        return true
    }
    
    override suspend fun isAvailable(): Boolean {
        return isInitialized
    }
    
    override suspend fun enhanceTaskDescription(
        accessibilityText: String,
        appPackage: String
    ): String? {
        if (!isInitialized) {
            Log.w(TAG, "Backend not initialized, cannot enhance task")
            return null
        }
        
        Log.d(TAG, "Enhancing task for app: $appPackage")
        Log.d(TAG, "Accessibility text: ${accessibilityText.take(100)}...")
        
        // Simulate processing delay (realistic AI inference time)
        val delay = Random.nextLong(MIN_DELAY_MS, MAX_DELAY_MS)
        delay(delay)
        
        // Simulate occasional failures to test fallback logic
        if (Random.nextFloat() < FAILURE_RATE) {
            Log.w(TAG, "Simulated AI failure for testing")
            return null
        }
        
        // Get enhanced description based on app package
        val enhancedTask = generateEnhancedTask(appPackage, accessibilityText)
        
        Log.d(TAG, "Generated enhanced task: $enhancedTask")
        return enhancedTask
    }
    
    /**
     * Generate enhanced task description based on app package and accessibility content.
     * 
     * This simulates what a real AI model would do:
     * 1. Analyze the app context
     * 2. Look for patterns in the accessibility text
     * 3. Generate meaningful task descriptions
     */
    private fun generateEnhancedTask(appPackage: String, accessibilityText: String): String {
        // Get app-specific patterns or use generic patterns
        val patterns = enhancementPatterns[appPackage] ?: getGenericPatterns(appPackage)
        
        // Try to create context-aware task based on accessibility text
        val contextualTask = createContextualTask(accessibilityText, patterns)
        if (contextualTask != null) {
            return contextualTask
        }
        
        // Fallback to random pattern for the app
        return patterns.random()
    }
    
    /**
     * Attempt to create contextual task based on accessibility text content.
     * This simulates basic text analysis that a real AI would perform.
     */
    private fun createContextualTask(accessibilityText: String, patterns: List<String>): String? {
        val lowerText = accessibilityText.lowercase()
        
        // Look for specific keywords to provide more contextual responses
        return when {
            lowerText.contains("compose") || lowerText.contains("new message") -> 
                "Compose new message or email"
            lowerText.contains("reply") || lowerText.contains("respond") -> 
                "Reply to received message"
            lowerText.contains("search") || lowerText.contains("find") -> 
                "Search for specific information"
            lowerText.contains("calendar") || lowerText.contains("schedule") -> 
                "Manage calendar appointment"
            lowerText.contains("photo") || lowerText.contains("camera") -> 
                "Capture or share photos"
            lowerText.contains("call") || lowerText.contains("phone") -> 
                "Make or answer phone call"
            else -> null
        }
    }
    
    /**
     * Generate generic patterns for apps not in our predefined list.
     * This ensures the mock backend can work with any app.
     */
    private fun getGenericPatterns(appPackage: String): List<String> {
        val appName = appPackage.split(".").lastOrNull()?.capitalize() ?: "App"
        return listOf(
            "Check latest updates in $appName",
            "Complete task in $appName",
            "Review notification from $appName",
            "Continue activity in $appName",
            "Respond to content in $appName"
        )
    }
    
    override suspend fun cleanup() {
        Log.d(TAG, "Cleaning up MockLLMBackend...")
        isInitialized = false
        // In a real implementation, this would free model resources
        Log.d(TAG, "MockLLMBackend cleanup complete")
    }
}