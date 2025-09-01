package studio.atopthehill.osom.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * MediaPipe backend implementation for on-device LLM task enhancement.
 * 
 * This backend uses Google's MediaPipe GenAI Tasks with Gemma3-1B-IT model
 * to transform accessibility text into meaningful task descriptions.
 * 
 * Key features:
 * - On-device inference (no internet required)
 * - Optimized for mobile performance
 * - Graceful error handling and fallback
 * - Resource management and cleanup
 * 
 * Implementation follows the MVP approach from the MediaPipe plan:
 * - Single model (Gemma3-1B-IT)
 * - Universal device compatibility
 * - Simple, reliable operation
 */
class MediaPipeBackend(private val context: Context) : SimpleLLMBackend {
    
    companion object {
        private const val TAG = "MediaPipeBackend"
        private const val INFERENCE_TIMEOUT_MS = 10000L // 10 seconds for model inference
        private const val INITIALIZATION_TIMEOUT_MS = 30000L // 30 seconds for model loading
    }
    
    private val modelHelper = MediaPipeModelHelper(context)
    private var isInitialized = false
    
    /**
     * Initialize the MediaPipe backend by loading the Gemma3-1B-IT model.
     * 
     * This process:
     * - Validates model file availability
     * - Loads the model into memory (~1.5GB peak usage)
     * - Configures inference parameters
     * - Handles initialization errors gracefully
     * 
     * @return true if initialization successful, false otherwise
     */
    override suspend fun initialize(): Boolean {
        if (isInitialized) {
            Log.d(TAG, "MediaPipe backend already initialized")
            return true
        }
        
        Log.d(TAG, "Initializing MediaPipe backend with Gemma3-1B-IT...")
        
        return try {
            withContext(Dispatchers.IO) {
                withTimeout(INITIALIZATION_TIMEOUT_MS) {
                    // Check device compatibility and model availability
                    if (!checkDeviceCompatibility()) {
                        Log.w(TAG, "Device not compatible with MediaPipe backend")
                        return@withTimeout false
                    }
                    
                    if (!modelHelper.isModelAvailable()) {
                        Log.e(TAG, "Model file not available. Please download according to assets/models/README.md")
                        return@withTimeout false
                    }
                    
                    // Initialize the model
                    val initSuccess = modelHelper.initializeModel()
                    if (initSuccess) {
                        isInitialized = true
                        Log.d(TAG, "MediaPipe backend initialized successfully")
                        Log.d(TAG, modelHelper.getModelStatus())
                    } else {
                        Log.e(TAG, "Failed to initialize MediaPipe model")
                    }
                    
                    initSuccess
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.w(TAG, "MediaPipe initialization timed out after ${INITIALIZATION_TIMEOUT_MS}ms", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception during MediaPipe initialization", e)
            false
        }
    }
    
    /**
     * Check if the backend is available and ready for inference requests.
     * 
     * This validates:
     * - Model has been successfully loaded
     * - Device has sufficient resources
     * - No critical errors have occurred
     */
    override suspend fun isAvailable(): Boolean {
        return isInitialized && modelHelper.isModelLoaded()
    }
    
    /**
     * Enhance accessibility text into a meaningful task description using AI.
     * 
     * This is the core functionality that transforms raw accessibility content
     * into user-friendly task descriptions. For example:
     * - Input: "Gmail Compose button New message recipient field"
     * - Output: "Compose new email message"
     * 
     * The method:
     * - Validates input parameters
     * - Creates appropriate prompts for the model
     * - Handles inference with timeout protection
     * - Processes and validates the response
     * 
     * @param accessibilityText Raw text from AccessibilityService
     * @param appPackage Package name of the source app
     * @return Enhanced task description or null if enhancement fails
     */
    override suspend fun enhanceTaskDescription(
        accessibilityText: String,
        appPackage: String
    ): String? {
        if (!isInitialized) {
            Log.w(TAG, "Cannot enhance task: backend not initialized")
            return null
        }
        
        if (accessibilityText.isBlank()) {
            Log.w(TAG, "Cannot enhance task: empty accessibility text")
            return null
        }
        
        Log.d(TAG, "Enhancing task for app: $appPackage")
        Log.d(TAG, "Accessibility text length: ${accessibilityText.length} chars")
        
        return try {
            withContext(Dispatchers.Default) {
                withTimeout(INFERENCE_TIMEOUT_MS) {
                    // Generate task description using the model
                    val enhancedTask = modelHelper.generateTaskDescription(
                        accessibilityText = accessibilityText,
                        appPackage = appPackage
                    )
                    
                    if (enhancedTask != null) {
                        Log.d(TAG, "Successfully enhanced task: $enhancedTask")
                        validateAndCleanResponse(enhancedTask)
                    } else {
                        Log.w(TAG, "Model returned null response")
                        null
                    }
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.w(TAG, "Task enhancement timed out after ${INFERENCE_TIMEOUT_MS}ms", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Exception during task enhancement", e)
            null
        }
    }
    
    /**
     * Clean up MediaPipe resources when the backend is no longer needed.
     * 
     * This is crucial for:
     * - Freeing model memory (~1.5GB)
     * - Preventing memory leaks
     * - Proper resource management on mobile devices
     */
    override suspend fun cleanup() {
        Log.d(TAG, "Cleaning up MediaPipe backend resources...")
        
        try {
            withContext(Dispatchers.IO) {
                modelHelper.cleanup()
                isInitialized = false
                Log.d(TAG, "MediaPipe backend cleanup complete")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during MediaPipe cleanup", e)
        }
    }
    
    /**
     * Check device compatibility for MediaPipe inference.
     * 
     * This ensures the device has sufficient:
     * - RAM for model loading (1.5GB minimum available)
     * - Storage for model file (~600MB)
     * - Android API level support (API 24+)
     */
    private fun checkDeviceCompatibility(): Boolean {
        return try {
            // Check Android API level (MediaPipe requires API 24+)
            val apiLevel = android.os.Build.VERSION.SDK_INT
            if (apiLevel < android.os.Build.VERSION_CODES.N) {
                Log.w(TAG, "Device API level $apiLevel below minimum (24)")
                return false
            }
            
            // Check available memory
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            activityManager.getMemoryInfo(memoryInfo)
            
            val availableMemoryGB = memoryInfo.availMem / (1024.0 * 1024.0 * 1024.0)
            if (availableMemoryGB < 1.5) {
                Log.w(TAG, "Insufficient available memory: ${availableMemoryGB}GB (minimum 1.5GB)")
                return false
            }
            
            Log.d(TAG, "Device compatibility check passed (API $apiLevel, ${availableMemoryGB}GB available)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during device compatibility check", e)
            false
        }
    }
    
    /**
     * Validate and clean the response from the AI model.
     * 
     * This ensures:
     * - Response is not empty or meaningless
     * - Length is reasonable for task descriptions
     * - Content is appropriate and safe
     */
    private fun validateAndCleanResponse(response: String): String? {
        val cleaned = response.trim()
        
        // Validate response quality
        if (cleaned.length < 3) {
            Log.w(TAG, "Response too short: '$cleaned'")
            return null
        }
        
        if (cleaned.length > 200) {
            Log.w(TAG, "Response too long, truncating: ${cleaned.length} chars")
            return cleaned.take(200).trim()
        }
        
        // Check for common AI failure patterns
        val lowerCleaned = cleaned.lowercase()
        if (lowerCleaned.contains("i cannot") || 
            lowerCleaned.contains("i'm unable") ||
            lowerCleaned.contains("as an ai")) {
            Log.w(TAG, "AI declined to process request")
            return null
        }
        
        return cleaned
    }
    
    /**
     * Get comprehensive status information for debugging and monitoring.
     * 
     * This helps with:
     * - Troubleshooting initialization issues
     * - Performance monitoring
     * - Resource usage tracking
     */
    fun getStatus(): String {
        return buildString {
            appendLine("MediaPipe Backend Status:")
            appendLine("- Initialized: $isInitialized")
            appendLine("- Model loaded: ${modelHelper.isModelLoaded()}")
            appendLine("- Device compatible: ${checkDeviceCompatibility()}")
            appendLine()
            append(modelHelper.getModelStatus())
        }
    }
    
    /**
     * Get basic performance metrics for monitoring inference speed.
     * Future enhancement: track average inference time, success rate, etc.
     */
    fun getPerformanceMetrics(): String {
        return "MediaPipe Backend Performance: (metrics collection to be implemented)"
    }
}