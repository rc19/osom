package studio.atopthehill.osom.ai

/**
 * Simple interface for LLM backends in Osom's MVP implementation.
 * 
 * This interface focuses on the core requirement: enhancing task descriptions
 * from accessibility content. The design is intentionally minimal to support
 * the MVP goal of replacing "Opened [AppName]" with meaningful task descriptions.
 * 
 * Future enhancements can extend this interface for more complex features like
 * contextual nudging, multiple backends, and advanced configuration options.
 */
interface SimpleLLMBackend {
    
    /**
     * Enhance a task description based on screen content from accessibility service.
     * 
     * This is the core function that transforms raw accessibility text into
     * meaningful task descriptions. For example:
     * - Raw text: "Gmail Compose button New message"
     * - Enhanced: "Reply to John's email about Q1 meeting"
     * 
     * @param accessibilityText Text extracted from the current screen via AccessibilityService
     * @param appPackage Package name of the source app (e.g., "com.google.android.gm")
     * @return Enhanced task description, or null if enhancement fails
     */
    suspend fun enhanceTaskDescription(
        accessibilityText: String,
        appPackage: String
    ): String?
    
    /**
     * Check if the backend is ready to process requests.
     * 
     * This allows the system to gracefully fallback to simple task creation
     * when the AI backend is not available or not properly initialized.
     * 
     * @return true if backend can process requests, false otherwise
     */
    suspend fun isAvailable(): Boolean
    
    /**
     * Initialize the backend if needed.
     * 
     * For mock implementations, this might be a no-op.
     * For real backends, this could involve model loading, authentication, etc.
     * 
     * @return true if initialization successful, false otherwise
     */
    suspend fun initialize(): Boolean
    
    /**
     * Clean up resources when the backend is no longer needed.
     * 
     * This ensures proper memory management and resource cleanup,
     * especially important for on-device model backends.
     */
    suspend fun cleanup()
}