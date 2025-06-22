package top.wxx9248.lspcarrotjuicer.controller

import android.content.Context
import android.util.Log
import androidx.core.content.edit

/**
 * Controller class for managing error-related shared preferences
 * Abstracts concrete storage interactions from business logic
 */
object ErrorController {
    private const val TAG = "LCJ/ErrorController"
    private const val PREFS_NAME = "lcj_errors"
    private const val KEY_LAST_ERROR = "last_error"
    private const val KEY_ERROR_TIMESTAMP = "error_timestamp"

    /**
     * Get the last known error from storage
     * @param context Application context
     * @return Last error message or null if none exists
     */
    fun getLastError(context: Context): String? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString(KEY_LAST_ERROR, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving last error", e)
            null
        }
    }

    /**
     * Save an error message to storage
     * @param context Application context
     * @param error Error message to save
     */
    fun setLastError(context: Context, error: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit {
                putString(KEY_LAST_ERROR, error)
                putLong(KEY_ERROR_TIMESTAMP, System.currentTimeMillis())
            }
            Log.d(TAG, "Error saved: $error")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving error message", e)
        }
    }

    /**
     * Clear the saved error from storage
     * @param context Application context
     */
    fun clearLastError(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit {
                remove(KEY_LAST_ERROR)
                remove(KEY_ERROR_TIMESTAMP)
            }
            Log.d(TAG, "Error cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing error message", e)
        }
    }

    /**
     * Get the timestamp when the last error was saved
     * @param context Application context
     * @return Timestamp in milliseconds or 0 if no error exists
     */
    fun getLastErrorTimestamp(context: Context): Long {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getLong(KEY_ERROR_TIMESTAMP, 0L)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving error timestamp", e)
            0L
        }
    }

    /**
     * Check if there is a saved error
     * @param context Application context
     * @return True if an error exists, false otherwise
     */
    fun hasError(context: Context): Boolean {
        return getLastError(context) != null
    }
}