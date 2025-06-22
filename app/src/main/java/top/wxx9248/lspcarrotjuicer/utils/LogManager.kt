package top.wxx9248.lspcarrotjuicer.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import top.wxx9248.lspcarrotjuicer.model.LogEntry
import top.wxx9248.lspcarrotjuicer.model.LogLevel
import java.util.concurrent.ConcurrentLinkedQueue

object LogManager {
    private const val MAX_LOG_ENTRIES = 1000

    private val logEntries = ConcurrentLinkedQueue<LogEntry>()
    private val _logFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    private val logMutex = Mutex()

    fun getAllLogs(): StateFlow<List<LogEntry>> = _logFlow.asStateFlow()

    suspend fun addLogEntry(logEntry: LogEntry) {
        logMutex.withLock {
            logEntries.offer(logEntry)

            // Maintain max size
            while (logEntries.size > MAX_LOG_ENTRIES) {
                logEntries.poll()
            }

            // Update flow
            _logFlow.value = logEntries.toList()
        }
    }

    suspend fun addLog(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        val logEntry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable?.stackTraceToString()
        )

        addLogEntry(logEntry)

        // Also log to Android's log system
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, message, throwable)
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }
    }

    suspend fun clearLogs() {
        logMutex.withLock {
            logEntries.clear()
            _logFlow.value = emptyList()
        }
    }

    // Convenience methods for different log levels
    suspend fun v(tag: String, message: String, throwable: Throwable? = null) {
        addLog(LogLevel.VERBOSE, tag, message, throwable)
    }

    suspend fun d(tag: String, message: String, throwable: Throwable? = null) {
        addLog(LogLevel.DEBUG, tag, message, throwable)
    }

    suspend fun i(tag: String, message: String, throwable: Throwable? = null) {
        addLog(LogLevel.INFO, tag, message, throwable)
    }

    suspend fun w(tag: String, message: String, throwable: Throwable? = null) {
        addLog(LogLevel.WARN, tag, message, throwable)
    }

    suspend fun e(tag: String, message: String, throwable: Throwable? = null) {
        addLog(LogLevel.ERROR, tag, message, throwable)
    }

    // Method for native layer to add logs
    @JvmStatic
    fun addNativeLog(levelInt: Int, tag: String, message: String) {
        val level = when (levelInt) {
            0 -> LogLevel.VERBOSE
            1 -> LogLevel.DEBUG
            2 -> LogLevel.INFO
            3 -> LogLevel.WARN
            4 -> LogLevel.ERROR
            else -> LogLevel.DEBUG
        }

        // Use coroutines in a background thread
        kotlinx.coroutines.GlobalScope.launch {
            addLog(level, tag, message)
        }
    }
} 
