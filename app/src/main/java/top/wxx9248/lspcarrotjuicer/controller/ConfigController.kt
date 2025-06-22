package top.wxx9248.lspcarrotjuicer.controller

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import top.wxx9248.lspcarrotjuicer.model.ServiceConfig
import androidx.core.content.edit

/**
 * Controller class for managing service configuration
 * Provides domain-specific property access instead of generic storage operations
 */
class ConfigController(private val context: Context) {
    companion object {
        private const val TAG = "LCJ/ConfigController"
        private const val PREFS_NAME = "lsp_carrot_juicer_config"
        private const val KEY_SERVICE_CONFIG = "service_config"

        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
    }

    private var _cachedConfig: ServiceConfig? = null

    /**
     * URA server address property
     */
    var uraServerAddress: String
        get() = getCurrentConfig().serverUrl
        set(value) {
            updateConfig { it.copy(serverUrl = value) }
        }

    /**
     * Service enabled state property
     */
    var isServiceEnabled: Boolean
        get() = getCurrentConfig().enabled
        set(value) {
            updateConfig { it.copy(enabled = value) }
        }

    /**
     * Connection timeout property (in milliseconds)
     */
    var connectionTimeout: Long
        get() = getCurrentConfig().timeout
        set(value) {
            updateConfig { it.copy(timeout = value) }
        }

    /**
     * Retry count property
     */
    var retryCount: Int
        get() = getCurrentConfig().retryCount
        set(value) {
            updateConfig { it.copy(retryCount = value) }
        }

    /**
     * Get the current complete configuration
     */
    fun getCurrentConfig(): ServiceConfig {
        if (_cachedConfig == null) {
            _cachedConfig = loadConfigFromStorage()
        }
        return _cachedConfig!!
    }

    /**
     * Update multiple configuration properties at once
     */
    fun updateConfiguration(block: (ServiceConfig) -> ServiceConfig): Boolean {
        return updateConfig(block)
    }

    /**
     * Reset configuration to defaults
     */
    fun resetToDefaults(): Boolean {
        return try {
            _cachedConfig = ServiceConfig()
            saveConfigToStorage(_cachedConfig!!)
            Log.d(TAG, "Configuration reset to defaults")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting configuration", e)
            false
        }
    }

    /**
     * Check if configuration has been customized from defaults
     */
    fun hasCustomConfiguration(): Boolean {
        val current = getCurrentConfig()
        val default = ServiceConfig()
        return current != default
    }

    private fun updateConfig(block: (ServiceConfig) -> ServiceConfig): Boolean {
        return try {
            val currentConfig = getCurrentConfig()
            val newConfig = block(currentConfig)
            _cachedConfig = newConfig
            saveConfigToStorage(newConfig)
            Log.d(TAG, "Configuration updated successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating configuration", e)
            false
        }
    }

    private fun loadConfigFromStorage(): ServiceConfig {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val configJson = prefs.getString(KEY_SERVICE_CONFIG, null)

            if (configJson != null) {
                try {
                    json.decodeFromString<ServiceConfig>(configJson)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse saved config, returning default", e)
                    ServiceConfig()
                }
            } else {
                Log.d(TAG, "No saved config found, returning default")
                ServiceConfig()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading service config", e)
            ServiceConfig()
        }
    }

    private fun saveConfigToStorage(config: ServiceConfig): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val configJson = json.encodeToString(config)
            
            prefs.edit {
                putString(KEY_SERVICE_CONFIG, configJson)
            }
            
            Log.d(TAG, "Service config saved successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving service config", e)
            false
        }
    }
}
