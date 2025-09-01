package studio.atopthehill.osom.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import java.io.File
import java.io.FileNotFoundException

/**
 * Helper class for managing MediaPipe LLM models and inference engines.
 * 
 * This class handles the complexity of:
 * - Model file validation and loading
 * - LlmInference configuration and lifecycle
 * - Device compatibility checking
 * - Resource management and cleanup
 * 
 * Based on Google AI Edge Gallery patterns for reliable model management.
 */
class MediaPipeModelHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "MediaPipeModelHelper"
        private const val MODEL_FILENAME = "gemma3-1b-it-int4.task"
        private const val MODEL_ASSET_PATH = "models/$MODEL_FILENAME"
        
        // Model configuration based on Gemma3-1B-IT specifications
        private const val MAX_TOKENS = 128 // Concise task descriptions
        private const val TEMPERATURE = 0.3f // Balanced creativity vs consistency
        private const val TOP_K = 40 // Focused output diversity
        private const val RANDOM_SEED = 101 // Consistent results for testing
    }
    
    private var llmInference: LlmInference? = null
    private var isModelLoaded = false
    
    /**
     * Check if the required model file exists and is accessible.
     * This validates the model before attempting to initialize MediaPipe.
     */
    fun isModelAvailable(): Boolean {
        return try {
            val inputStream = context.assets.open(MODEL_ASSET_PATH)
            val isAvailable = inputStream.available() > 0
            inputStream.close()
            
            if (isAvailable) {
                Log.d(TAG, "Model file found: $MODEL_FILENAME")
            } else {
                Log.w(TAG, "Model file exists but appears empty: $MODEL_FILENAME")
            }
            
            isAvailable
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "Model file not found: $MODEL_FILENAME. Please download it according to assets/models/README.md")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking model availability", e)
            false
        }
    }
    
    /**
     * Initialize the MediaPipe LLM inference engine with the Gemma3-1B-IT model.
     * 
     * This method:
     * - Validates model availability
     * - Configures LlmInference with optimal settings for task enhancement
     * - Handles initialization errors gracefully
     * 
     * @return true if initialization successful, false otherwise
     */
    suspend fun initializeModel(): Boolean {
        if (isModelLoaded) {
            Log.d(TAG, "Model already loaded")
            return true
        }
        
        return try {
            Log.d(TAG, "Initializing MediaPipe LLM with Gemma3-1B-IT...")
            
            // Configure LlmInference options based on Google AI Edge Gallery patterns
            // MediaPipe requires absolute path, so we need to copy from assets to internal storage
            val modelFile = copyModelToInternalStorage()
            if (modelFile == null) {
                Log.e(TAG, "Failed to copy model from assets to internal storage")
                return false
            }
            
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(MAX_TOKENS)
                .build()
            
            Log.d(TAG, "Model file path: ${modelFile.absolutePath}")
            Log.d(TAG, "Model file size: ${modelFile.length()} bytes")
            Log.d(TAG, "Model file readable: ${modelFile.canRead()}")
            
            Log.d(TAG, "Starting MediaPipe model loading...")
            val startTime = System.currentTimeMillis()
            
            try {
                // Create LlmInference instance - this loads the model
                llmInference = LlmInference.createFromOptions(context, options)
                
                val loadTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "MediaPipe model loaded successfully in ${loadTime}ms")
            } catch (e: Exception) {
                val loadTime = System.currentTimeMillis() - startTime
                Log.e(TAG, "MediaPipe model loading failed after ${loadTime}ms", e)
                throw e
            }
            isModelLoaded = true
            
            Log.d(TAG, "MediaPipe LLM initialized successfully")
            Log.d(TAG, "Model configuration: maxTokens=$MAX_TOKENS, temperature=$TEMPERATURE, topK=$TOP_K")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe LLM", e)
            llmInference = null
            isModelLoaded = false
            false
        }
    }
    
    /**
     * Create a new inference session for generating task descriptions.
     * 
     * Each session should be used for a single inference task and then closed.
     * This follows the MediaPipe pattern for session management.
     * 
     * @return LlmInferenceSession if successful, null if model not loaded
     */
    private fun createInferenceSession(): LlmInferenceSession? {
        return try {
            if (!isModelLoaded || llmInference == null) {
                Log.w(TAG, "Cannot create session: model not loaded")
                return null
            }
            
            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(TOP_K)
                .setTemperature(TEMPERATURE)
                .build()
                
            val session = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions)
            Log.d(TAG, "Created new inference session")
            session
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create inference session", e)
            null
        }
    }
    
    /**
     * Generate enhanced task description using the loaded model.
     * 
     * This method:
     * - Creates a session for the inference
     * - Builds an appropriate prompt for task enhancement
     * - Handles the inference process
     * - Cleans up resources properly
     * 
     * @param accessibilityText Raw text from AccessibilityService
     * @param appPackage Package name of the source app
     * @return Enhanced task description or null if generation fails
     */
    suspend fun generateTaskDescription(
        accessibilityText: String,
        appPackage: String
    ): String? {
        if (!isModelLoaded) {
            Log.w(TAG, "Cannot generate: model not loaded")
            return null
        }
        
        var session: LlmInferenceSession? = null
        return try {
            session = createInferenceSession()
            if (session == null) {
                Log.w(TAG, "Failed to create inference session")
                return null
            }
            
            // Build task enhancement prompt
            val prompt = buildTaskEnhancementPrompt(accessibilityText, appPackage)
            Log.d(TAG, "Generating task description for app: ${getAppNameFromPackage(appPackage)}")
            
            // Add prompt to session and generate response
            session.addQueryChunk(prompt)
            val response = session.generateResponse()
            
            // Process and validate the response
            val enhancedTask = processResponse(response)
            Log.d(TAG, "Generated enhanced task: $enhancedTask")
            
            enhancedTask
        } catch (e: Exception) {
            Log.e(TAG, "Error during task description generation", e)
            null
        }
    }
    
    /**
     * Build an effective prompt for task enhancement based on accessibility content.
     * 
     * The prompt is designed to:
     * - Convert raw accessibility text into actionable tasks
     * - Maintain context about the source app
     * - Generate concise, user-friendly descriptions
     */
    private fun buildTaskEnhancementPrompt(accessibilityText: String, appPackage: String): String {
        val appName = getAppNameFromPackage(appPackage)
        
        return buildString {
            appendLine("Task: Transform this screen content into a clear, actionable task description in a pirate's voice.")
            appendLine("App: $appName")
            appendLine("Screen content: \"${accessibilityText.take(300)}\"")
            appendLine()
            appendLine("Guidelines:")
            appendLine("- Create a specific, actionable task (e.g., 'Reply to John's email about meeting')")
            appendLine("- Keep it under 50 words")
            appendLine("- Make it user-friendly and contextual")
            appendLine("- If unclear, create a general task for the app")
            appendLine()
            appendLine("Enhanced task:")
        }
    }
    
    /**
     * Process and clean up the raw response from the LLM.
     * This ensures consistent output formatting.
     */
    private fun processResponse(rawResponse: String): String {
        return rawResponse
            .trim()
            .removePrefix("Enhanced task:")
            .removePrefix("Task:")
            .trim()
            .take(200) // Ensure reasonable length
            .ifBlank { "Continue activity in app" } // Fallback for empty responses
    }
    
    /**
     * Extract a user-friendly app name from the package name.
     * This helps create better prompts and more contextual tasks.
     */
    private fun getAppNameFromPackage(packageName: String): String {
        return when (packageName) {
            "com.google.android.gm" -> "Gmail"
            "com.google.android.calendar" -> "Calendar"
            "com.android.chrome", "com.chrome.beta" -> "Chrome"
            "com.google.android.apps.messaging" -> "Messages"
            "com.whatsapp" -> "WhatsApp"
            "com.spotify.music" -> "Spotify"
            "com.instagram.android" -> "Instagram"
            else -> {
                // Extract app name from package (e.g. "com.example.app" -> "App")
                packageName.substringAfterLast(".")
                    .replaceFirstChar { it.uppercaseChar() }
                    .ifBlank { "App" }
            }
        }
    }
    
    /**
     * Copy model from assets to internal storage since MediaPipe needs absolute file path.
     * This is only done once - subsequent calls check if file already exists.
     */
    private fun copyModelToInternalStorage(): File? {
        return try {
            val internalModelDir = File(context.filesDir, "models")
            if (!internalModelDir.exists()) {
                internalModelDir.mkdirs()
            }
            
            val modelFile = File(internalModelDir, MODEL_FILENAME)
            
            // Only copy if file doesn't exist or is corrupted
            if (!modelFile.exists() || modelFile.length() == 0L) {
                Log.d(TAG, "Copying model from assets to internal storage...")
                
                context.assets.open(MODEL_ASSET_PATH).use { inputStream ->
                    modelFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                Log.d(TAG, "Model copied successfully: ${modelFile.absolutePath} (${modelFile.length()} bytes)")
            } else {
                Log.d(TAG, "Using existing model file: ${modelFile.absolutePath}")
            }
            
            modelFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy model to internal storage", e)
            null
        }
    }

    /**
     * Check if the model is currently loaded and ready for inference.
     */
    fun isModelLoaded(): Boolean = isModelLoaded
    
    /**
     * Get current model status for debugging and monitoring.
     */
    fun getModelStatus(): String {
        return buildString {
            appendLine("MediaPipe Model Status:")
            appendLine("- Model file available: ${isModelAvailable()}")
            appendLine("- Model loaded: $isModelLoaded")
            appendLine("- Model path: $MODEL_ASSET_PATH")
            appendLine("- Configuration: maxTokens=$MAX_TOKENS, temperature=$TEMPERATURE")
        }
    }
    
    /**
     * Clean up model resources and free memory.
     * This should be called when the backend is no longer needed.
     */
    fun cleanup() {
        try {
            // MediaPipe LlmInference doesn't have explicit close method
            // Resources are managed automatically by the library
            llmInference = null
            isModelLoaded = false
            Log.d(TAG, "Model resources cleaned up")
        } catch (e: Exception) {
            Log.w(TAG, "Error during model cleanup", e)
        }
    }
}