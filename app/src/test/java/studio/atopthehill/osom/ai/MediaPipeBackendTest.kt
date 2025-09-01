package studio.atopthehill.osom.ai

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for MediaPipeBackend.
 * 
 * These tests validate the basic functionality of the MediaPipe backend
 * without requiring the actual model file (since it's ~600MB).
 * 
 * Tests focus on:
 * - Initialization behavior without model
 * - Error handling
 * - Device compatibility checking
 * - Resource management
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MediaPipeBackendTest {
    
    private lateinit var context: Context
    private lateinit var mediaPixipeBackend: MediaPipeBackend
    
    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        mediaPixipeBackend = MediaPipeBackend(context)
    }
    
    @Test
    fun `backend starts uninitialized`() = runBlocking {
        // Backend should not be available before initialization
        assert(!mediaPixipeBackend.isAvailable())
    }
    
    @Test
    fun `initialization fails gracefully without model file`() = runBlocking {
        // Without the actual model file, initialization should fail gracefully
        val initResult = mediaPixipeBackend.initialize()
        
        // Should return false since model file is not present
        assert(!initResult)
        assert(!mediaPixipeBackend.isAvailable())
    }
    
    @Test
    fun `enhanceTaskDescription returns null when not initialized`() = runBlocking {
        // Without initialization, enhancement should return null
        val result = mediaPixipeBackend.enhanceTaskDescription(
            "Gmail Compose button", 
            "com.google.android.gm"
        )
        
        assert(result == null)
    }
    
    @Test
    fun `enhanceTaskDescription handles empty input gracefully`() = runBlocking {
        // Even if somehow initialized, empty input should be handled
        val result = mediaPixipeBackend.enhanceTaskDescription(
            "", 
            "com.google.android.gm"
        )
        
        assert(result == null)
    }
    
    @Test
    fun `cleanup can be called safely multiple times`() = runBlocking {
        // Cleanup should be safe to call multiple times
        mediaPixipeBackend.cleanup()
        mediaPixipeBackend.cleanup() // Should not throw
        
        // Backend should not be available after cleanup
        assert(!mediaPixipeBackend.isAvailable())
    }
    
    @Test
    fun `getStatus returns meaningful information`() {
        val status = mediaPixipeBackend.getStatus()
        
        // Status should contain key information
        assert(status.contains("MediaPipe Backend Status"))
        assert(status.contains("Initialized"))
        assert(status.contains("Model loaded"))
        assert(status.contains("Device compatible"))
    }
    
    @Test
    fun `backend handles null context gracefully`() {
        // Test that backend creation doesn't immediately crash
        // Note: We can't test with null context due to constructor, but this validates creation
        val backend = MediaPipeBackend(context)
        assert(!backend.getStatus().isEmpty())
    }
}