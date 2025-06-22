package top.wxx9248.lspcarrotjuicer.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Service state enumeration
 */
enum class ServiceState {
    STARTING,
    READY,
    ERROR,
    STOPPING,
    STOPPED
}

/**
 * Service configuration
 */
@Serializable
@Parcelize
data class ServiceConfig(
    val enabled: Boolean = true,
    val serverUrl: String = "",
    val timeout: Long = 5000L,
    val retryCount: Int = 3
) : Parcelable

/**
 * Packet data structure for IPC communication
 */
@Serializable
data class PacketData(
    val isRequest: Boolean,
    val data: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PacketData

        if (isRequest != other.isRequest) return false
        if (!data.contentEquals(other.data)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isRequest.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Log entry for display in UI
 */
@Serializable
data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: String? = null
)

enum class LogLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR
}

/**
 * Module status information
 */
data class ModuleStatus(
    val isXposedActive: Boolean = false,
    val isServiceRunning: Boolean = false,
    val serviceState: ServiceState = ServiceState.STOPPED,
    val lastError: String? = null
) 
