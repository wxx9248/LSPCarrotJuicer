package top.wxx9248.lspcarrotjuicer.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import top.wxx9248.lspcarrotjuicer.model.PacketData
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Robust packet sender with HTTP/HTTPS support and proper error handling
 */
class PacketSender {
    companion object {
        private const val TAG = "LCJ/PacketSender"
        private const val CONNECT_TIMEOUT = 10000 // 10 seconds
        private const val READ_TIMEOUT = 15000 // 15 seconds
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY = 1000L // 1 second

        // HTTP headers
        private const val CONTENT_TYPE = "application/octet-stream"
        private const val USER_AGENT = "LSPCarrotJuicer/2.0"
    }

    private val senderScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isShutdown = AtomicBoolean(false)
    private val sendMutex = Mutex()

    suspend fun sendPacket(packet: PacketData, serverUrl: String): Boolean {
        if (isShutdown.get()) {
            Log.w(TAG, "Sender is shutdown, dropping packet")
            return false
        }

        return sendMutex.withLock {
            var lastException: Exception? = null

            repeat(MAX_RETRIES) { attempt ->
                try {
                    val success = sendPacketInternal(packet, serverUrl)
                    if (success) {
                        if (attempt > 0) {
                            Log.i(TAG, "Packet sent successfully on retry $attempt")
                        }
                        return@withLock true
                    }
                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "Packet send attempt ${attempt + 1} failed", e)

                    if (attempt < MAX_RETRIES - 1) {
                        delay(RETRY_DELAY * (attempt + 1)) // Exponential backoff
                    }
                }
            }

            Log.e(TAG, "Failed to send packet after $MAX_RETRIES attempts", lastException)
            false
        }
    }

    private suspend fun sendPacketInternal(packet: PacketData, serverUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null

            try {
                val url = URL(buildPacketUrl(serverUrl, packet))
                connection = url.openConnection() as HttpURLConnection

                configureConnection(connection)

                // Write packet data
                connection.doOutput = true
                connection.outputStream.use { outputStream ->
                    outputStream.write(packet.data)
                    outputStream.flush()
                }

                val responseCode = connection.responseCode
                val success = responseCode in 200..299

                if (success) {
                    Log.v(TAG, "Packet sent successfully, response: $responseCode")
                } else {
                    Log.w(TAG, "Server returned error: $responseCode")
                }

                success

            } catch (e: IOException) {
                Log.e(TAG, "Network error sending packet", e)
                false
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error sending packet", e)
                false
            } finally {
                connection?.disconnect()
            }
        }
    }

    suspend fun testConnectivity(serverUrl: String): Boolean {
        if (isShutdown.get()) {
            return false
        }

        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null

            try {
                val testUrl = URL(buildTestUrl(serverUrl))
                connection = testUrl.openConnection() as HttpURLConnection

                configureConnection(connection)
                connection.requestMethod = "HEAD" // Just check if server is reachable
                connection.doOutput = false

                val responseCode = connection.responseCode
                val success = responseCode in 200..299

                Log.i(TAG, "Connectivity test: $serverUrl -> $responseCode ($success)")
                success

            } catch (e: IOException) {
                Log.w(TAG, "Connectivity test failed: $serverUrl", e)
                false
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during connectivity test", e)
                false
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun configureConnection(connection: HttpURLConnection) {
        connection.connectTimeout = CONNECT_TIMEOUT
        connection.readTimeout = READ_TIMEOUT
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", CONTENT_TYPE)
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", "*/*")
        connection.setRequestProperty("Connection", "close")
        connection.useCaches = false
        connection.instanceFollowRedirects = true
    }

    private fun buildPacketUrl(serverUrl: String, packet: PacketData): String {
        val baseUrl = serverUrl.removeSuffix("/")
        val endpoint = if (packet.isRequest) "request" else "response"
        return "$baseUrl/$endpoint"
    }

    private fun buildTestUrl(serverUrl: String): String {
        val baseUrl = serverUrl.removeSuffix("/")
        return "$baseUrl/health" // Or whatever health check endpoint the URA server provides
    }

    fun shutdown() {
        Log.i(TAG, "Shutting down packet sender")
        isShutdown.set(true)
        senderScope.cancel()
    }
} 
