package com.secureguard.mdm.utils

import android.content.Context
import com.secureguard.mdm.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileLogger {

    private lateinit var appContext: Context
    private val scope = CoroutineScope(Dispatchers.IO)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun init(context: Context) {
        appContext = context.applicationContext
        cleanOldLogs()
    }

    /**
     * Log to both standard Logcat and the dual File logger.
     * @param tag The log tag
     * @param message The message
     * @param isDebug If true, this goes only to the debug log. If false, it goes to both debug and production logs.
     */
    @JvmStatic
    @JvmOverloads
    fun log(tag: String, message: String, isDebug: Boolean = false) {
        if (!::appContext.isInitialized) {
            AppLogger.e("FileLogger", "FileLogger not initialized. Cannot log: [$tag] $message")
            return
        }

        scope.launch {
            try {
                // Always log to standard logcat
                AppLogger.d(tag, message)

                val dateStr = fileDateFormat.format(Date())
                val timeStr = timeFormat.format(Date())
                val logMessage = "$timeStr [$tag]: $message\n"

                // Define files in internal storage
                val logsDir = File(appContext.filesDir, "logs")
                if (!logsDir.exists()) logsDir.mkdirs()

                // Write to debug log (contains EVERYTHING)
                val debugFile = File(logsDir, "debug_log_$dateStr.txt")
                appendLog(debugFile, logMessage)

                // Write to production log (contains only NON-debug messages)
                if (!isDebug) {
                    val prodFile = File(logsDir, "production_log_$dateStr.txt")
                    appendLog(prodFile, logMessage)
                }

            } catch (e: Exception) {
                AppLogger.e("FileLogger", "Failed to write to log file", e)
            }
        }
    }

    private fun appendLog(file: File, message: String) {
        // Rotate if a single day's log somehow exceeds 5MB
        if (file.exists() && file.length() > 5 * 1024 * 1024) {
            val backupFile = File(file.parent, file.name.replace(".txt", "_old.txt"))
            if(backupFile.exists()) backupFile.delete()
            file.renameTo(backupFile)
        }
        file.appendText(message)
    }

    private fun cleanOldLogs() {
        scope.launch {
            try {
                val logsDir = File(appContext.filesDir, "logs")
                if (!logsDir.exists()) return@launch

                val retentionDays = 7
                val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)

                logsDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < cutoffTime) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("FileLogger", "Failed to clean old logs", e)
            }
        }
    }
}