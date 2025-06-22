package top.wxx9248.lspcarrotjuicer.utils

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.wxx9248.lspcarrotjuicer.model.ServiceConfig
import top.wxx9248.lspcarrotjuicer.network.PacketSender
import top.wxx9248.lspcarrotjuicer.service.PacketForwardingService

object ServiceManager {

    fun updateConfig(context: Context, config: ServiceConfig) {
        val intent = Intent(context, PacketForwardingService::class.java).apply {
            action = PacketForwardingService.ACTION_UPDATE_CONFIG
            putExtra(PacketForwardingService.EXTRA_CONFIG, config)
        }
        context.startService(intent)
    }

    suspend fun testConnectivity(context: Context, serverUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val packetSender = PacketSender()
                packetSender.testConnectivity(serverUrl)
            } catch (e: Exception) {
                false
            }
        }
    }

    fun startService(context: Context, config: ServiceConfig) {
        val intent = Intent(context, PacketForwardingService::class.java).apply {
            action = PacketForwardingService.ACTION_START
            putExtra(PacketForwardingService.EXTRA_CONFIG, config)
        }
        context.startForegroundService(intent)
    }

    fun stopService(context: Context) {
        val intent = Intent(context, PacketForwardingService::class.java).apply {
            action = PacketForwardingService.ACTION_STOP
        }
        context.startService(intent)
    }
} 
