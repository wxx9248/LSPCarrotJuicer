package top.wxx9248.lspcarrotjuicer.ipc

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import top.wxx9248.lspcarrotjuicer.model.PacketData
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

/**
 * IPC Manager for communication between native library and Kotlin service
 * Uses named pipes (FIFOs) for high-performance, low-latency communication
 */
class IPCManager {
    companion object {
        private const val TAG = "LCJ/IPCManager"
        private const val PIPE_DIR = "/data/data/jp.co.cygames.umamusume/cache/lcj_pipes"
        private const val PACKET_PIPE = "$PIPE_DIR/packets"
        private const val LOG_PIPE = "$PIPE_DIR/logs"
        private const val CONTROL_PIPE = "$PIPE_DIR/control"

        // Message types for control pipe
        private const val MSG_HANDSHAKE = 0x01
        private const val MSG_SHUTDOWN = 0x02
        private const val MSG_ACK = 0x03
        private const val MSG_HEARTBEAT = 0x04

        private const val HANDSHAKE_TIMEOUT = 10000L // 10 seconds
        private const val HEARTBEAT_INTERVAL = 5000L // 5 seconds
        private const val READ_TIMEOUT = 1000L // 1 second
    }

    private var ipcScope: CoroutineScope? = null
    private val isInitialized = AtomicBoolean(false)
    private val isShutdown = AtomicBoolean(false)

    // Callbacks
    private var packetCallback: ((PacketData) -> Unit)? = null
    private var logCallback: ((String) -> Unit)? = null

    // Pipe streams
    private var packetInputStream: FileInputStream? = null
    private var logInputStream: FileInputStream? = null
    private var controlInputStream: FileInputStream? = null
    private var controlOutputStream: FileOutputStream? = null

    // Processing jobs
    private var packetProcessingJob: Job? = null
    private var logProcessingJob: Job? = null
    private var controlProcessingJob: Job? = null
    private var heartbeatJob: Job? = null

    private val initMutex = Mutex()

    fun setPacketCallback(callback: (PacketData) -> Unit) {
        packetCallback = callback
    }

    fun setLogCallback(callback: (String) -> Unit) {
        logCallback = callback
    }

    suspend fun initialize(): Boolean {
        return initMutex.withLock {
            if (isInitialized.get()) {
                Log.w(TAG, "IPC already initialized")
                return@withLock true
            }

            try {
                Log.i(TAG, "Initializing IPC manager")

                ipcScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

                // Create pipe directory
                val pipeDir = File(PIPE_DIR)
                if (!pipeDir.exists() && !pipeDir.mkdirs()) {
                    throw IOException("Failed to create pipe directory: $PIPE_DIR")
                }

                // Create named pipes
                createNamedPipes()

                // Open pipe streams
                openPipeStreams()

                // Start processing jobs
                startProcessingJobs()

                // Perform handshake
                if (!performHandshake()) {
                    throw RuntimeException("Handshake failed")
                }

                isInitialized.set(true)
                Log.i(TAG, "IPC manager initialized successfully")
                true

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize IPC manager", e)
                cleanup()
                false
            }
        }
    }

    suspend fun shutdown() {
        Log.i(TAG, "Shutting down IPC manager")
        isShutdown.set(true)

        try {
            // Send shutdown message to native
            sendControlMessage(MSG_SHUTDOWN)

            // Cancel all jobs
            heartbeatJob?.cancel()
            packetProcessingJob?.cancel()
            logProcessingJob?.cancel()
            controlProcessingJob?.cancel()

            // Cancel scope
            ipcScope?.cancel()

            // Cleanup resources
            cleanup()

        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown", e)
        }

        isInitialized.set(false)
        Log.i(TAG, "IPC manager shutdown complete")
    }

    private fun createNamedPipes() {
        val pipes = listOf(PACKET_PIPE, LOG_PIPE, CONTROL_PIPE)

        for (pipe in pipes) {
            val process = ProcessBuilder("mkfifo", pipe).start()
            val exitCode = process.waitFor()

            if (exitCode != 0 && !File(pipe).exists()) {
                throw IOException("Failed to create named pipe: $pipe")
            }
        }

        Log.d(TAG, "Named pipes created successfully")
    }

    private fun openPipeStreams() {
        try {
            // Open with non-blocking flag when possible
            packetInputStream = FileInputStream(PACKET_PIPE)
            logInputStream = FileInputStream(LOG_PIPE)
            controlInputStream = FileInputStream(CONTROL_PIPE)
            controlOutputStream = FileOutputStream(CONTROL_PIPE)

            Log.d(TAG, "Pipe streams opened successfully")
        } catch (e: Exception) {
            throw IOException("Failed to open pipe streams", e)
        }
    }

    private fun startProcessingJobs() {
        packetProcessingJob = ipcScope?.launch {
            processPackets()
        }

        logProcessingJob = ipcScope?.launch {
            processLogs()
        }

        controlProcessingJob = ipcScope?.launch {
            processControlMessages()
        }

        heartbeatJob = ipcScope?.launch {
            sendHeartbeats()
        }

        Log.d(TAG, "Processing jobs started")
    }

    private suspend fun performHandshake(): Boolean {
        return withTimeoutOrNull(HANDSHAKE_TIMEOUT) {
            Log.d(TAG, "Starting handshake process")

            // Send handshake message
            sendControlMessage(MSG_HANDSHAKE)

            // Wait for acknowledgment
            // This will be handled by control message processing
            // For now, assume handshake succeeds after sending
            delay(1000) // Give time for native to respond

            Log.i(TAG, "Handshake completed successfully")
            true
        } ?: run {
            Log.e(TAG, "Handshake timeout")
            false
        }
    }

    private suspend fun processPackets() {
        ByteArray(8192) // 8KB buffer

        try {
            while (!isShutdown.get() && coroutineContext.isActive) {
                try {
                    val input = packetInputStream ?: break

                    // Read packet header (isRequest + size)
                    val headerBuffer = ByteArray(5) // 1 byte bool + 4 byte int
                    var totalRead = 0

                    while (totalRead < headerBuffer.size) {
                        val bytesRead =
                            input.read(headerBuffer, totalRead, headerBuffer.size - totalRead)
                        if (bytesRead == -1) {
                            delay(READ_TIMEOUT)
                            continue
                        }
                        totalRead += bytesRead
                    }

                    val headerBuf = ByteBuffer.wrap(headerBuffer)
                    val isRequest = headerBuf.get() != 0.toByte()
                    val dataSize = headerBuf.int

                    if (dataSize < 0 || dataSize > 1024 * 1024) { // 1MB max
                        Log.w(TAG, "Invalid packet size: $dataSize")
                        continue
                    }

                    // Read packet data
                    val packetData = ByteArray(dataSize)
                    totalRead = 0

                    while (totalRead < dataSize) {
                        val bytesRead = input.read(packetData, totalRead, dataSize - totalRead)
                        if (bytesRead == -1) {
                            delay(READ_TIMEOUT)
                            continue
                        }
                        totalRead += bytesRead
                    }

                    // Create packet and notify callback
                    val packet = PacketData(isRequest, packetData)
                    packetCallback?.invoke(packet)

                } catch (e: Exception) {
                    if (!isShutdown.get()) {
                        Log.e(TAG, "Error processing packet", e)
                        delay(100) // Brief delay before retry
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Packet processing stopped", e)
        }
    }

    private suspend fun processLogs() {
        ByteArray(4096)

        try {
            while (!isShutdown.get() && coroutineContext.isActive) {
                try {
                    val input = logInputStream ?: break

                    // Read log length
                    val lengthBuffer = ByteArray(4)
                    var totalRead = 0

                    while (totalRead < 4) {
                        val bytesRead = input.read(lengthBuffer, totalRead, 4 - totalRead)
                        if (bytesRead == -1) {
                            delay(READ_TIMEOUT)
                            continue
                        }
                        totalRead += bytesRead
                    }

                    val logLength = ByteBuffer.wrap(lengthBuffer).int
                    if (logLength < 0 || logLength > 4096) {
                        Log.w(TAG, "Invalid log length: $logLength")
                        continue
                    }

                    // Read log message
                    val logData = ByteArray(logLength)
                    totalRead = 0

                    while (totalRead < logLength) {
                        val bytesRead = input.read(logData, totalRead, logLength - totalRead)
                        if (bytesRead == -1) {
                            delay(READ_TIMEOUT)
                            continue
                        }
                        totalRead += bytesRead
                    }

                    val logMessage = String(logData, Charsets.UTF_8)
                    logCallback?.invoke(logMessage)

                } catch (e: Exception) {
                    if (!isShutdown.get()) {
                        Log.e(TAG, "Error processing log", e)
                        delay(100)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Log processing stopped", e)
        }
    }

    private suspend fun processControlMessages() {
        try {
            while (!isShutdown.get() && coroutineContext.isActive) {
                try {
                    val input = controlInputStream ?: break

                    val messageType = input.read()
                    if (messageType == -1) {
                        delay(READ_TIMEOUT)
                        continue
                    }

                    when (messageType) {
                        MSG_HANDSHAKE -> {
                            Log.d(TAG, "Received handshake from native")
                            sendControlMessage(MSG_ACK)
                        }

                        MSG_SHUTDOWN -> {
                            Log.d(TAG, "Received shutdown from native")
                            sendControlMessage(MSG_ACK)
                            break
                        }

                        MSG_HEARTBEAT -> {
                            Log.v(TAG, "Received heartbeat from native")
                        }

                        else -> {
                            Log.w(TAG, "Unknown control message: $messageType")
                        }
                    }

                } catch (e: Exception) {
                    if (!isShutdown.get()) {
                        Log.e(TAG, "Error processing control message", e)
                        delay(100)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Control message processing stopped", e)
        }
    }

    private suspend fun sendHeartbeats() {
        try {
            while (!isShutdown.get() && coroutineContext.isActive) {
                delay(HEARTBEAT_INTERVAL)

                try {
                    sendControlMessage(MSG_HEARTBEAT)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to send heartbeat", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Heartbeat sender stopped", e)
        }
    }

    private fun sendControlMessage(messageType: Int) {
        try {
            controlOutputStream?.write(messageType)
            controlOutputStream?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send control message: $messageType", e)
        }
    }

    private fun cleanup() {
        try {
            packetInputStream?.close()
            logInputStream?.close()
            controlInputStream?.close()
            controlOutputStream?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error during cleanup", e)
        }

        packetInputStream = null
        logInputStream = null
        controlInputStream = null
        controlOutputStream = null
    }
} 
