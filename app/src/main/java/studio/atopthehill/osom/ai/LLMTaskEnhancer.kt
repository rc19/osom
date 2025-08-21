package studio.atopthehill.osom.ai

import android.util.Log
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import studio.atopthehill.osom.data.repository.AppRepository

/**
 * Task Enhancer wrapper for LLM integration in Osom's MVP implementation.
 * 
 * This class provides a clean interface between the AccessibilityService and the AI backend,
 * handling timeout, fallback logic, and settings management. It's designed to be simple
 * and reliable for the MVP while providing hooks for future enhancements.
 * 
 * Key responsibilities:
 * - Check if AI is enabled in user settings
 * - Call AI backend with proper timeout handling
 * - Provide graceful fallback to simple task creation
 * - Log AI performance for debugging and optimization
 */
class LLMTaskEnhancer(
    private val appRepository: AppRepository,
    private val llmBackend: SimpleLLMBackend = MockLLMBackend()
) {
    companion object {
        private const val TAG = "LLMTaskEnhancer"
        private const val AI_TIMEOUT_MS = 5000L // 5 seconds timeout for AI calls
    }
    
    private var isInitialized = false
    
    /**
     * Initialize the AI backend if needed.
     * This should be called when the service starts to ensure the backend is ready.
     */
    suspend fun initialize(): Boolean {
        if (isInitialized) return true
        
        Log.d(TAG, "Initializing LLM task enhancer...")
        return try {
            withTimeout(AI_TIMEOUT_MS) {
                val success = llmBackend.initialize()
                if (success) {
                    isInitialized = true
                    Log.d(TAG, "LLM task enhancer initialized successfully")
                } else {
                    Log.w(TAG, "Failed to initialize LLM backend")
                }
                success
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "LLM backend initialization timed out", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception during LLM backend initialization", e)
            false
        }
    }
    
    /**
     * Enhance a task description using AI if enabled, otherwise return simple fallback.
     * 
     * This is the main method called by AccessibilityService to get task descriptions.
     * It handles all the complexity of checking settings, calling AI, and fallback logic.
     * 
     * @param accessibilityText Text extracted from the current screen
     * @param appPackage Package name of the source app
     * @param appName Human-readable app name for fallback
     * @return Enhanced task description or simple fallback
     */
    suspend fun enhanceTask(
        accessibilityText: String,
        appPackage: String,
        appName: String
    ): String {
        // Check if AI is enabled in user settings
        val isAIEnabled = try {
            appRepository.isAITasksEnabled()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check AI settings, defaulting to disabled", e)
            false
        }
        
        if (!isAIEnabled) {
            Log.d(TAG, "AI tasks disabled, using simple logic")
            return createSimpleTask(appName)
        }
        
        // Ensure backend is initialized
        if (!isInitialized && !initialize()) {
            Log.w(TAG, "Backend not initialized, falling back to simple task")
            return createSimpleTask(appName)
        }
        
        // Check if backend is available
        if (!llmBackend.isAvailable()) {
            Log.w(TAG, "Backend not available, falling back to simple task")
            return createSimpleTask(appName)
        }
        
        // Try to get AI-enhanced task description
        return try {
            withTimeout(AI_TIMEOUT_MS) {
                val enhancedTask = llmBackend.enhanceTaskDescription(accessibilityText, appPackage)
                
                if (enhancedTask.isNullOrBlank()) {
                    Log.d(TAG, "AI returned empty result, using fallback")
                    createSimpleTask(appName)
                } else {
                    Log.d(TAG, "AI enhanced task created: $enhancedTask")
                    enhancedTask
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "AI call timed out, using fallback", e)
            createSimpleTask(appName)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during AI task enhancement", e)
            createSimpleTask(appName)
        }
    }
    
    /**
     * Create simple task description as fallback when AI is disabled or fails.
     * This matches the current behavior in AccessibilityService.
     */
    private fun createSimpleTask(appName: String): String {
        return "Opened $appName"
    }
    
    /**
     * Check if AI task enhancement is currently enabled.
     * Useful for AccessibilityService to optimize text extraction.
     */
    suspend fun isAIEnabled(): Boolean {
        return try {
            appRepository.isAITasksEnabled()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check AI status", e)
            false
        }
    }
    
    /**
     * Clean up resources when the enhancer is no longer needed.
     * Should be called when the service is destroyed.
     */
    suspend fun cleanup() {
        Log.d(TAG, "Cleaning up LLM task enhancer...")
        try {
            llmBackend.cleanup()
            isInitialized = false
            Log.d(TAG, "LLM task enhancer cleanup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Exception during cleanup", e)
        }
    }
    
    /**
     * Get basic status information for debugging and monitoring.
     * TODO: Add performance metrics in future iterations.
     */
    fun getStatus(): String {
        return "LLMTaskEnhancer(initialized=$isInitialized, backend=${llmBackend.javaClass.simpleName})"
    }
}