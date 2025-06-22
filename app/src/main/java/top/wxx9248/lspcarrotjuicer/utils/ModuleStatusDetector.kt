package top.wxx9248.lspcarrotjuicer.utils

import android.content.Context
import android.util.Log
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import top.wxx9248.lspcarrotjuicer.model.ModuleStatus
import top.wxx9248.lspcarrotjuicer.model.ServiceState
import top.wxx9248.lspcarrotjuicer.controller.ErrorController

object ModuleStatusDetector {
    private const val TAG = "LCJ/ModuleStatusDetector"
    private var xposedService: XposedService? = null

    init {
        // Try to get the Xposed service for version detection
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                xposedService = service
                Log.d(TAG, "XposedService connected: ${service.frameworkName} v${service.frameworkVersion}")
            }

            override fun onServiceDied(service: XposedService) {
                if (xposedService == service) {
                    xposedService = null
                    Log.d(TAG, "XposedService died")
                }
            }
        })
    }

    /**
     * Detect if the module is active in LSPosed framework
     * This uses the modern libxposed API to check framework status
     */
    fun isXposedActive(): Boolean {
        return try {
            val xposedVersion = getXposedVersion()
            val isActive = xposedVersion > 0
            Log.d(TAG, "Xposed framework check: version=$xposedVersion, active=$isActive")
            isActive
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Xposed status", e)
            false
        }
    }

    /**
     * Check if the packet forwarding service is running
     * Uses ServiceStateTracker instead of deprecated getRunningServices()
     */
    fun isServiceRunning(context: Context): Boolean {
        return ServiceStateTracker.isPacketForwardingServiceRunning
    }

    /**
     * Singleton to track service state as replacement for deprecated getRunningServices()
     */
    object ServiceStateTracker {
        @Volatile
        var isPacketForwardingServiceRunning: Boolean = false
            private set

        fun setServiceRunning(running: Boolean) {
            isPacketForwardingServiceRunning = running
        }
    }

    /**
     * Get the current service state
     */
    fun getServiceState(context: Context): ServiceState {
        return if (isServiceRunning(context)) {
            // If service is running, we assume it's ready
            // In a real implementation, you might want to bind to the service
            // and get the actual state
            ServiceState.READY
        } else {
            ServiceState.STOPPED
        }
    }

    /**
     * Get comprehensive module status
     */
    fun getModuleStatus(context: Context): ModuleStatus {
        return try {
            val isXposedActive = isXposedActive()
            val isServiceRunning = isServiceRunning(context)
            val serviceState = getServiceState(context)
            val lastError = getLastError(context)

            Log.i(
                TAG,
                "Module status: xposed=$isXposedActive, service=$isServiceRunning, state=$serviceState"
            )

            // Additional validation - if Xposed is active but service won't start, there might be an issue
            val finalError = when {
                !isXposedActive -> "Xposed framework not active or module not loaded"
                isXposedActive && !isServiceRunning && serviceState == ServiceState.ERROR ->
                    lastError ?: "Service failed to start despite active Xposed framework"
                else -> lastError
            }

            ModuleStatus(
                isXposedActive = isXposedActive,
                isServiceRunning = isServiceRunning,
                serviceState = serviceState,
                lastError = finalError
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting module status", e)
            saveError(context, "Failed to determine module status: ${e.message}")
            ModuleStatus(
                isXposedActive = false,
                isServiceRunning = false,
                serviceState = ServiceState.ERROR,
                lastError = "Failed to determine module status: ${e.message}"
            )
        }
    }

    /**
     * Get last known error from storage
     */
    fun getLastError(context: Context): String? {
        return ErrorController.getLastError(context)
    }

    /**
     * Save error to storage
     */
    fun saveError(context: Context, error: String) {
        ErrorController.setLastError(context, error)
    }

    /**
     * Clear saved error
     */
    fun clearError(context: Context) {
        ErrorController.clearLastError(context)
    }

    /**
     * Get the Xposed framework version if available
     * @return The version number, or 0 if not available
     */
    fun getXposedVersion(): Int {
        return try {
            val service = xposedService
            if (service != null) {
                val version = service.apiVersion
                Log.d(TAG, "Modern API version: $version")
                version
            } else {
                Log.d(TAG, "XposedService not available")
                0
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error getting API version: ${e.message}")
            0
        }
    }
}
