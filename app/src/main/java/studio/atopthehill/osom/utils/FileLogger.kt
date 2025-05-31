package studio.atopthehill.osom.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object FileLogger {

    private const val TAG = "FileLogger"
    private val FILE_NAME_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    private val LOG_ENTRY_TIMESTAMP_FORMAT =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    @Volatile private var currentLogFile: File? = null
    @Volatile private var writer: BufferedWriter? = null
    @Volatile private var currentSessionStartTime: LocalDateTime? = null
    @Volatile private var currentAppLabel: String? = null

    @Synchronized
    fun startSession(context: Context, appLabel: String, sessionStartTime: LocalDateTime) {
        if (writer != null) {
            Log.w(
                    TAG,
                    "Attempted to start a new session while one is already active. Closing previous one."
            )
            endSession(
                    LocalDateTime.now()
            ) // Pass only endTime, context not needed for directory determination here
        }

        this.currentSessionStartTime = sessionStartTime
        this.currentAppLabel = appLabel

        // Define tempFileName first so it's in scope
        val tempFileName =
                "${appLabel.replace("[^a-zA-Z0-9.-]", "_")}_${sessionStartTime.format(FILE_NAME_DATE_FORMAT)}_PENDING.txt"

        // Get the public Downloads directory
        val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appSpecificLogDirName = "OSOM_Session_Logs"
        var logsDir =
                File(downloadsDir, appSpecificLogDirName) // Store in a subfolder within Downloads

        var attemptInternalFallback = false
        if (!logsDir.exists()) {
            if (!logsDir.mkdirs()) {
                Log.e(
                        TAG,
                        "Failed to create directory in Downloads: ${logsDir.absolutePath}. Attempting internal fallback."
                )
                attemptInternalFallback = true
            }
        }

        if (attemptInternalFallback) {
            val internalFallbackDir = File(context.filesDir, "session_logs_fallback")
            if (!internalFallbackDir.exists()) {
                internalFallbackDir.mkdirs() // Create fallback if it doesn't exist
            }
            logsDir = internalFallbackDir // Use fallback directory
            Log.w(TAG, "Logging to internal fallback directory: ${logsDir.absolutePath}")
        }

        currentLogFile = File(logsDir, tempFileName)

        try {
            writer = BufferedWriter(FileWriter(currentLogFile, true)) // Append mode
            Log.i(TAG, "Session started. Logging to: ${currentLogFile?.absolutePath}")
            log(
                    "FileLogger",
                    "SESSION_START: App: $appLabel, Start: ${sessionStartTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}"
            )
        } catch (e: IOException) {
            Log.e(TAG, "Error opening log file: ${currentLogFile?.absolutePath}", e)
            writer = null
            currentLogFile = null // Ensure this is nulled out on error
        } catch (e: SecurityException) {
            Log.e(
                    TAG,
                    "SecurityException opening log file: ${currentLogFile?.absolutePath}. Check permissions.",
                    e
            )
            writer = null
            currentLogFile = null
        }
    }

    @Synchronized
    fun log(logSourceTag: String, message: String) {
        if (writer == null || currentLogFile == null) {
            return
        }
        try {
            val timestamp = LOG_ENTRY_TIMESTAMP_FORMAT.format(Date())
            writer?.append("$timestamp [$logSourceTag] $message\n")
            writer?.flush()
        } catch (e: IOException) {
            Log.e(TAG, "Error writing to log file: ${currentLogFile?.absolutePath}", e)
        }
    }

    @Synchronized
    fun endSession(
            sessionEndTime: LocalDateTime
    ) { // Removed context as it's not used for path here
        if (writer == null ||
                        currentLogFile == null ||
                        currentSessionStartTime == null ||
                        currentAppLabel == null
        ) {
            Log.w(TAG, "endSession called but no active session or necessary info is missing.")
            closeWriter()
            return
        }

        log(
                "FileLogger",
                "SESSION_END: App: $currentAppLabel, End: ${sessionEndTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}"
        )
        closeWriter()

        val finalFileName =
                "${currentAppLabel!!.replace("[^a-zA-Z0-9.-]", "_")}_${currentSessionStartTime!!.format(FILE_NAME_DATE_FORMAT)}_${sessionEndTime.format(FILE_NAME_DATE_FORMAT)}.txt"

        val actualLogsDir = currentLogFile?.parentFile

        if (actualLogsDir == null) {
            Log.e(
                    TAG,
                    "Cannot determine log directory for finalization. Temp file path: ${currentLogFile?.absolutePath}"
            )
            currentLogFile = null
            currentSessionStartTime = null
            currentAppLabel = null
            return
        }

        val finalFile = File(actualLogsDir, finalFileName)

        if (currentLogFile?.exists() == true) {
            if (currentLogFile!!.renameTo(finalFile)) {
                Log.i(TAG, "Session ended. Log file finalized: ${finalFile.absolutePath}")
            } else {
                Log.e(
                        TAG,
                        "Error renaming log file from ${currentLogFile?.name} to ${finalFile.name}. Source: ${currentLogFile?.absolutePath}, Target: ${finalFile.absolutePath}"
                )
            }
        } else {
            Log.w(
                    TAG,
                    "Original temp log file not found for renaming: ${currentLogFile?.absolutePath}"
            )
        }

        currentLogFile = null
        currentSessionStartTime = null
        currentAppLabel = null
    }

    private fun closeWriter() {
        try {
            writer?.flush()
            writer?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing log file writer.", e)
        }
        writer = null
    }
}
