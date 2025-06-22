package top.wxx9248.lspcarrotjuicer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import top.wxx9248.lspcarrotjuicer.R
import top.wxx9248.lspcarrotjuicer.model.PacketData
import top.wxx9248.lspcarrotjuicer.model.ServiceConfig
import top.wxx9248.lspcarrotjuicer.model.ServiceState
import top.wxx9248.lspcarrotjuicer.ipc.IPCManager
import top.wxx9248.lspcarrotjuicer.network.PacketSender
import top.wxx9248.lspcarrotjuicer.ui.activity.MainActivity
import top.wxx9248.lspcarrotjuicer.utils.ModuleStatusDetector
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class PacketForwardingService : Service() {
    companion object {
        private const val TAG = "LCJ/PacketForwardingService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "packet_forwarding"

        // Service actions
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE_CONFIG = "ACTION_UPDATE_CONFIG"
        const val EXTRA_CONFIG = "EXTRA_CONFIG"

        // Service states
        const val STATE_STARTING = 0
        const val STATE_READY = 1
        const val STATE_ERROR = 2
        const val STATE_STOPPING = 3
        const val STATE_STOPPED = 4
    }

    private val binder = PacketForwardingBinder()
    private var serviceScope: CoroutineScope? = null

    // Thread-safe state management
    private val currentState = AtomicReference(ServiceState.STARTING)
    private val isEnabled = AtomicBoolean(true)
    private val configMutex = Mutex()
    private var currentConfig = AtomicReference<ServiceConfig?>(null)

    // Components
    private var ipcManager: IPCManager? = null
    private var packetSender: PacketSender? = null

    // Packet processing
    private val packetChannel = Channel<PacketData>(capacity = Channel.UNLIMITED)
    private var packetProcessingJob: Job? = null

    // Service callbacks
    private val serviceCallbacks = mutableSetOf<ServiceCallback>()
    private val callbackMutex = Mutex()

    inner class PacketForwardingBinder : Binder() {
        fun getService(): PacketForwardingService = this@PacketForwardingService
    }

    interface ServiceCallback {
        suspend fun onStateChanged(newState: ServiceState)
        suspend fun onConfigChanged(config: ServiceConfig)
        suspend fun onError(error: String, throwable: Throwable?)
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")

        // Update service state tracker
        ModuleStatusDetector.ServiceStateTracker.setServiceRunning(true)

        serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(ServiceState.STARTING))

        // Initialize service components
        serviceScope?.launch {
            try {
                initializeService()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize service", e)
                handleError("Service initialization failed", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        Log.i(TAG, "Service destroying")

        // Update service state tracker
        ModuleStatusDetector.ServiceStateTracker.setServiceRunning(false)

        serviceScope?.launch {
            shutdownService()
        }

        serviceScope?.cancel()
        serviceScope = null

        super.onDestroy()
    }

    private suspend fun initializeService() {
        Log.d(TAG, "Initializing service components")

        // Initialize IPC manager
        ipcManager = IPCManager().apply {
            setPacketCallback { packetData ->
                // Queue packet immediately for processing
                val result = packetChannel.trySend(packetData)
                if (result.isFailure) {
                    Log.e(TAG, "Failed to queue packet", result.exceptionOrNull())
                }
            }

            setLogCallback { logMessage ->
                // Handle log from native layer
                Log.i("LCJ/Native", logMessage)
            }
        }

        // Initialize packet sender
        packetSender = PacketSender()

        // Start packet processing coroutine
        startPacketProcessing()

        // Initialize IPC
        if (ipcManager?.initialize() == true) {
            updateState(ServiceState.READY)
            Log.i(TAG, "Service initialized successfully")
        } else {
            throw RuntimeException("Failed to initialize IPC")
        }
    }

    private suspend fun shutdownService() {
        Log.d(TAG, "Shutting down service")
        updateState(ServiceState.STOPPING)

        // Stop packet processing
        packetProcessingJob?.cancel()
        packetChannel.close()

        // Shutdown IPC
        ipcManager?.shutdown()

        // Shutdown packet sender
        packetSender?.shutdown()

        updateState(ServiceState.STOPPED)
        Log.i(TAG, "Service shutdown complete")
    }

    private fun startPacketProcessing() {
        packetProcessingJob = serviceScope?.launch {
            Log.d(TAG, "Starting packet processing coroutine")

            try {
                for (packet in packetChannel) {
                    if (!isEnabled.get()) {
                        Log.d(TAG, "Packet dropped - service disabled")
                        continue
                    }

                    val config = currentConfig.get()
                    if (config == null) {
                        Log.w(TAG, "Packet dropped - no configuration")
                        continue
                    }

                    try {
                        packetSender?.sendPacket(packet, config.serverUrl)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send packet", e)
                        // Don't break the loop for individual packet failures
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Packet processing error", e)
                handleError("Packet processing failed", e)
            }
        }
    }

    suspend fun updateConfig(newConfig: ServiceConfig): Boolean {
        return configMutex.withLock {
            try {
                // Test connectivity if URL changed
                val oldConfig = currentConfig.get()
                if (oldConfig == null || oldConfig.serverUrl != newConfig.serverUrl) {
                    val testResult = packetSender?.testConnectivity(newConfig.serverUrl) == true
                    if (!testResult) {
                        return@withLock false
                    }
                }

                currentConfig.set(newConfig)
                isEnabled.set(newConfig.enabled)

                notifyConfigChanged(newConfig)
                Log.i(TAG, "Configuration updated successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update configuration", e)
                handleError("Configuration update failed", e)
                false
            }
        }
    }

    suspend fun getCurrentState(): ServiceState = currentState.get()

    suspend fun getCurrentConfig(): ServiceConfig? = currentConfig.get()

    suspend fun testConnectivity(url: String): Boolean {
        return try {
            packetSender?.testConnectivity(url) == true
        } catch (e: Exception) {
            Log.e(TAG, "Connectivity test failed", e)
            false
        }
    }

    suspend fun addCallback(callback: ServiceCallback) {
        callbackMutex.withLock {
            serviceCallbacks.add(callback)
        }
    }

    suspend fun removeCallback(callback: ServiceCallback) {
        callbackMutex.withLock {
            serviceCallbacks.remove(callback)
        }
    }

    private suspend fun updateState(newState: ServiceState) {
        val oldState = currentState.getAndSet(newState)
        if (oldState != newState) {
            Log.d(TAG, "State changed: $oldState -> $newState")

            // Update notification
            val notification = createNotification(newState)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

            // Notify callbacks
            notifyStateChanged(newState)
        }
    }

    private suspend fun handleError(message: String, throwable: Throwable?) {
        Log.e(TAG, message, throwable)
        updateState(ServiceState.ERROR)
        // Update service state tracker on error
        ModuleStatusDetector.ServiceStateTracker.setServiceRunning(false)
        notifyError(message, throwable)
    }

    private suspend fun notifyStateChanged(newState: ServiceState) {
        callbackMutex.withLock {
            serviceCallbacks.forEach { callback ->
                try {
                    callback.onStateChanged(newState)
                } catch (e: Exception) {
                    Log.e(TAG, "Callback error", e)
                }
            }
        }
    }

    private suspend fun notifyConfigChanged(config: ServiceConfig) {
        callbackMutex.withLock {
            serviceCallbacks.forEach { callback ->
                try {
                    callback.onConfigChanged(config)
                } catch (e: Exception) {
                    Log.e(TAG, "Callback error", e)
                }
            }
        }
    }

    private suspend fun notifyError(message: String, throwable: Throwable?) {
        callbackMutex.withLock {
            serviceCallbacks.forEach { callback ->
                try {
                    callback.onError(message, throwable)
                } catch (e: Exception) {
                    Log.e(TAG, "Callback error", e)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Packet Forwarding Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "LSPCarrotJuicer packet forwarding service"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(state: ServiceState): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, text, icon) = when (state) {
            ServiceState.STARTING -> Triple(
                "Starting...",
                "Initializing packet forwarding",
                R.drawable.icon_foreground
            )

            ServiceState.READY -> Triple(
                "Active",
                "Packet forwarding active",
                R.drawable.icon_foreground
            )

            ServiceState.ERROR -> Triple(
                "Error",
                "Service encountered an error",
                R.drawable.icon_foreground
            )

            ServiceState.STOPPING -> Triple(
                "Stopping...",
                "Shutting down service",
                R.drawable.icon_foreground
            )

            ServiceState.STOPPED -> Triple("Stopped", "Service stopped", R.drawable.icon_foreground)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }
}
