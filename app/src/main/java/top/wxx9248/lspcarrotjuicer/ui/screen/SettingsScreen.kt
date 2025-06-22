package top.wxx9248.lspcarrotjuicer.ui.screen


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.wxx9248.lspcarrotjuicer.controller.ConfigController
import top.wxx9248.lspcarrotjuicer.utils.ServiceManager

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configController = remember { ConfigController(context) }

    // State management
    var isModuleEnabled by remember { mutableStateOf(true) }
    var serverUrl by remember { mutableStateOf(TextFieldValue("")) }
    var isLoading by remember { mutableStateOf(true) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Boolean?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf<Boolean?>(null) }

    // Load initial configuration
    LaunchedEffect(Unit) {
        try {
            isModuleEnabled = configController.isServiceEnabled
            serverUrl = TextFieldValue(configController.uraServerAddress)
        } catch (e: Exception) {
            // Handle loading error
        }
        isLoading = false
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        item {
            if (!isLoading) {
                ModuleControlCard(
                    isEnabled = isModuleEnabled,
                    onEnabledChange = { newValue ->
                        isModuleEnabled = newValue
                        scope.launch {
                            try {
                                configController.isServiceEnabled = newValue
                                val currentConfig = configController.getCurrentConfig()
                                ServiceManager.updateConfig(context, currentConfig)
                            } catch (e: Exception) {
                                // Handle error
                            }
                        }
                    }
                )
            }
        }

        item {
            if (!isLoading) {
                ServerConfigCard(
                    serverUrl = serverUrl,
                    onUrlChange = { serverUrl = it },
                    isTesting = isTesting,
                    testResult = testResult,
                    onTest = {
                        scope.launch {
                            isTesting = true
                            testResult = null
                            try {
                                val result =
                                    ServiceManager.testConnectivity(context, serverUrl.text)
                                testResult = result
                            } catch (e: Exception) {
                                testResult = false
                            }
                            isTesting = false
                        }
                    },
                    isSaving = isSaving,
                    saveSuccess = saveSuccess,
                    onSave = {
                        scope.launch {
                            isSaving = true
                            saveSuccess = null
                            try {
                                configController.uraServerAddress = serverUrl.text
                                val newConfig = configController.getCurrentConfig()
                                ServiceManager.updateConfig(context, newConfig)
                                saveSuccess = true
                            } catch (e: Exception) {
                                saveSuccess = false
                            }
                            isSaving = false
                        }
                    }
                )
            }
        }

        item {
            if (!isLoading) {
                AboutCard()
            }
        }
    }
}

@Composable
fun ModuleControlCard(
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Module Control",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Module",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isEnabled) {
                            "Module is actively forwarding packets to the URA server"
                        } else {
                            "Module is disabled. Packets will be dropped and not forwarded"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onEnabledChange
                )
            }
        }
    }
}

@Composable
fun ServerConfigCard(
    serverUrl: TextFieldValue,
    onUrlChange: (TextFieldValue) -> Unit,
    isTesting: Boolean,
    testResult: Boolean?,
    onTest: () -> Unit,
    isSaving: Boolean,
    saveSuccess: Boolean?,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Server Configuration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = serverUrl,
                onValueChange = onUrlChange,
                label = { Text("URA Server URL") },
                placeholder = { Text("https://example.com:8080") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isTesting && !isSaving
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter the full URL including protocol (http:// or https://) and port if needed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Test result display
            testResult?.let { result ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = if (result) Icons.Default.Check else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (result) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (result) "Server is reachable" else "Server is not reachable",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (result) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }

            // Save result display
            saveSuccess?.let { success ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = if (success) Icons.Default.Check else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (success) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (success) "Configuration saved successfully" else "Failed to save configuration",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (success) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onTest,
                    enabled = !isTesting && !isSaving && serverUrl.text.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isTesting) "Testing..." else "Test Connection")
                }

                Button(
                    onClick = onSave,
                    enabled = !isTesting && !isSaving && serverUrl.text.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(if (isSaving) "Saving..." else "Save")
                }
            }
        }
    }
}

@Composable
fun AboutCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "About",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AboutItem(label = "App Version", value = "2.0.0")
            Spacer(modifier = Modifier.height(8.dp))
            AboutItem(label = "Author", value = "wxx9248")
        }
    }
}

@Composable
fun AboutItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    MaterialTheme {
        Surface {
            SettingsScreen()
        }
    }
}
