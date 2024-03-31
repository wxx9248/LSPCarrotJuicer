package top.wxx9248.lspcarrotjuicer.xposed

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout

@SuppressLint("InflateParams")
fun showSettingsDialog(targetActivity: Activity, resources: Resources) {
    val logTag = "LCJ/SettingsDialog/showSettingsDialog"
    val configDirectoryPath = "${targetActivity.noBackupFilesDir}/LCJ"

    val config = readConfig(configDirectoryPath)

    val layout = LinearLayout(targetActivity)
    layout.orientation = LinearLayout.VERTICAL
    layout.setPadding(64, 32, 64, 32)

    val builder = AlertDialog.Builder(targetActivity)
    builder.setTitle(targetActivity.resources.getString(resources.stringAppName))
    builder.setCancelable(false)

    val inputHost = EditText(targetActivity)
    inputHost.hint =
        targetActivity.resources.getString(resources.stringSettingsDialogInputHostPrompt)
    inputHost.setText(config.host)
    layout.addView(inputHost)

    val inputPort = EditText(targetActivity)
    inputPort.hint =
        targetActivity.resources.getString(resources.stringSettingsDialogInputPortPrompt)
    inputPort.setText(config.port)
    layout.addView(inputPort)

    builder.setView(layout)

    builder.setPositiveButton("Confirm", DialogInterface.OnClickListener { dialog, which ->
        val host = inputHost.text.toString().trim()
        val port = inputPort.text.toString().trim()
        Log.d(logTag, "Host: $host")
        Log.d(logTag, "Port: $port")

        saveConfig(configDirectoryPath, Config(host, port))
    })

    builder.show()
}

