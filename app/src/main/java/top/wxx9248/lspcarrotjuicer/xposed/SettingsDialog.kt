package top.wxx9248.lspcarrotjuicer.xposed

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout

@SuppressLint("InflateParams")
fun showSettingsDialog(targetActivity: Activity) {
    val logTag = "LCJ/SettingsDialog/showSettingsDialog"
    val configDirectoryPath = "${targetActivity.noBackupFilesDir}/LCJ"

    val config = readConfig(configDirectoryPath)

    val layout = LinearLayout(targetActivity)
    layout.orientation = LinearLayout.VERTICAL
    layout.setPadding(64, 32, 64, 32)

    val builder = AlertDialog.Builder(targetActivity)
    builder.setTitle("LSPCarrotJuicer")
    builder.setCancelable(false)

    val inputHost = EditText(targetActivity)
    inputHost.hint = "Host"
    inputHost.setText(config.host)
    layout.addView(inputHost)

    val inputPort = EditText(targetActivity)
    inputPort.hint = "Port"
    inputPort.setText(config.port)
    layout.addView(inputPort)

    builder.setView(layout)

    builder.setPositiveButton("Confirm") { _, _ ->
        val host = inputHost.text.toString().trim()
        val port = inputPort.text.toString().trim()
        Log.d(logTag, "Host: $host")
        Log.d(logTag, "Port: $port")

        saveConfig(configDirectoryPath, Config(host, port))
    }

    builder.show()
}

