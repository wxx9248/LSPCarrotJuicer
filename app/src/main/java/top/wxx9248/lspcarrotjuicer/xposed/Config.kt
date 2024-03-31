package top.wxx9248.lspcarrotjuicer.xposed

import android.util.Log
import java.io.File
import java.io.FileNotFoundException

data class Config(
    val host: String,
    val port: String
)

fun readConfig(configDirectoryPath: String): Config {
    val logTag = "LCJ/Config/readConfig"

    Log.d(logTag, "Config directory at $configDirectoryPath")

    var host = ""
    try {
        host = File("$configDirectoryPath/host").readText()
        Log.d(logTag, "Read host $host from config")
    } catch (_: FileNotFoundException) {
        Log.i(logTag, "Config file host does not exist")
    }

    var port = ""
    try {
        port = File("$configDirectoryPath/port").readText()
        Log.d(logTag, "Read port $port from config")
    } catch (_: FileNotFoundException) {
        Log.i(logTag, "Config file port does not exist")
    }

    return Config(host, port)
}

fun saveConfig(configDirectoryPath: String, config: Config) {
    val logTag = "LCJ/Config/saveConfig"

    File(configDirectoryPath).mkdirs()
    Log.d(logTag, "Config directory at $configDirectoryPath")

    File("$configDirectoryPath/host").writeText(config.host)
    File("$configDirectoryPath/port").writeText(config.port)
    Log.d(logTag, "Config files saved")
}
