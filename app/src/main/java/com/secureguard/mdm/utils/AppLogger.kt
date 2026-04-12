package com.secureguard.mdm.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class LogEntry(
    val timestamp: Long,
    val level: String,
    val tag: String,
    val message: String,
    val isVpn: Boolean
)

object AppLogger {
    private const val MAX_LOGS_PER_CATEGORY = 2000
    private const val FILE_NAME = "app_logs.json"
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var logFile: File? = null
    
    private val appLogs = mutableListOf<LogEntry>()
    private val vpnLogs = mutableListOf<LogEntry>()
    
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    private var isDirty = false

    private val json = Json { ignoreUnknownKeys = true }

    fun init(context: Context) {
        logFile = File(context.filesDir, FILE_NAME)
        loadLogs()
        startSaveLoop()
    }

    private fun startSaveLoop() {
        scope.launch {
            while (isActive) {
                delay(5000)
                if (isDirty) {
                    saveLogs()
                    isDirty = false
                }
            }
        }
    }
    
    // Sniff based on tag or caller class for VPN classification
    private fun isVpnLog(tag: String?): Boolean {
        val t = tag?.lowercase() ?: ""
        if (t.contains("vpn") || t.contains("capture") || t.contains("pcap") || t.contains("firewall") || t.contains("netfree") || t.contains("rimon")) {
            return true
        }
        
        // Inspect stack trace to see if the log originated from a VPN module
        val stackTrace = Thread.currentThread().stackTrace
        for (element in stackTrace) {
            val cls = element.className
            if (cls.startsWith("com.secureguard.mdm.utils.AppLogger") || cls.startsWith("java.lang.Thread")) {
                continue // Skip our own logging frames
            }
            if (cls.startsWith("com.emanuelef.remote_capture") ||
                cls.startsWith("com.secureguard.mdm.vpn") ||
                cls.startsWith("com.secureguard.mdm.firewall") ||
                cls.contains("netfree") || 
                cls.contains("rimon")) {
                return true
            }
            // Once we hit the immediate caller outside of AppLogger, we can stop checking or keep going.
            // Breaking after the first real caller saves performance.
            break 
        }
        return false
    }

    private fun addLog(level: String, tag: String?, msg: String?, tr: Throwable? = null) {
        val safeTag = tag ?: "UNKNOWN"
        val message = buildString {
            if (msg != null) append(msg)
            if (tr != null) {
                if (isNotEmpty()) append("\n")
                append(Log.getStackTraceString(tr))
            }
        }
        
        val isVpn = isVpnLog(safeTag)
        val entry = LogEntry(System.currentTimeMillis(), level, safeTag, message, isVpn)
        
        scope.launch {
            synchronized(this@AppLogger) {
                if (isVpn) {
                    vpnLogs.add(0, entry)
                    if (vpnLogs.size > MAX_LOGS_PER_CATEGORY) vpnLogs.removeLast()
                } else {
                    appLogs.add(0, entry)
                    if (appLogs.size > MAX_LOGS_PER_CATEGORY) appLogs.removeLast()
                }
                isDirty = true
                
                val combined = (appLogs + vpnLogs).sortedByDescending { it.timestamp }
                _logsFlow.value = combined
            }
        }
        
        // Pass through to android.util.Log
        when (level) {
            "V" -> if (tr != null) Log.v(safeTag, message, tr) else Log.v(safeTag, message)
            "D" -> if (tr != null) Log.d(safeTag, message, tr) else Log.d(safeTag, message)
            "I" -> if (tr != null) Log.i(safeTag, message, tr) else Log.i(safeTag, message)
            "W" -> if (tr != null) Log.w(safeTag, message, tr) else Log.w(safeTag, message)
            "E" -> if (tr != null) Log.e(safeTag, message, tr) else Log.e(safeTag, message)
        }
    }

    private fun saveLogs() {
        logFile?.let { file ->
            try {
                val combined: List<LogEntry>
                synchronized(this@AppLogger) {
                    combined = appLogs + vpnLogs
                }
                file.writeText(json.encodeToString(combined))
            } catch (e: Exception) {
                Log.e("AppLogger", "Failed to save logs to JSON", e)
            }
        }
    }

    private fun loadLogs() {
        scope.launch {
            logFile?.let { file ->
                if (file.exists()) {
                    try {
                        val content = file.readText()
                        val loaded: List<LogEntry> = json.decodeFromString(content)
                        synchronized(this@AppLogger) {
                            appLogs.clear()
                            vpnLogs.clear()
                            loaded.forEach { 
                                if (it.isVpn) vpnLogs.add(it) else appLogs.add(it) 
                            }
                            
                            // Sort them initially after loading
                            appLogs.sortByDescending { it.timestamp }
                            vpnLogs.sortByDescending { it.timestamp }

                            while(appLogs.size > MAX_LOGS_PER_CATEGORY) appLogs.removeLast()
                            while(vpnLogs.size > MAX_LOGS_PER_CATEGORY) vpnLogs.removeLast()
                            
                            val combined = (appLogs + vpnLogs).sortedByDescending { it.timestamp }
                            _logsFlow.value = combined
                        }
                    } catch (e: Exception) {
                        Log.e("AppLogger", "Failed to load logs", e)
                    }
                }
            }
        }
    }

    @JvmStatic fun v(tag: String?, msg: String?) = addLog("V", tag, msg)
    @JvmStatic fun v(tag: String?, msg: String?, tr: Throwable?) = addLog("V", tag, msg, tr)
    @JvmStatic fun d(tag: String?, msg: String?) = addLog("D", tag, msg)
    @JvmStatic fun d(tag: String?, msg: String?, tr: Throwable?) = addLog("D", tag, msg, tr)
    @JvmStatic fun i(tag: String?, msg: String?) = addLog("I", tag, msg)
    @JvmStatic fun i(tag: String?, msg: String?, tr: Throwable?) = addLog("I", tag, msg, tr)
    @JvmStatic fun w(tag: String?, msg: String?) = addLog("W", tag, msg)
    @JvmStatic fun w(tag: String?, msg: String?, tr: Throwable?) = addLog("W", tag, msg, tr)
    @JvmStatic fun e(tag: String?, msg: String?) = addLog("E", tag, msg)
    @JvmStatic fun e(tag: String?, msg: String?, tr: Throwable?) = addLog("E", tag, msg, tr)
}
