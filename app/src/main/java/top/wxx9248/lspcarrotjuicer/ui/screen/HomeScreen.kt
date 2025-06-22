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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.wxx9248.lspcarrotjuicer.model.ModuleStatus
import top.wxx9248.lspcarrotjuicer.model.ServiceState
import top.wxx9248.lspcarrotjuicer.utils.ModuleStatusDetector

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var moduleStatus by remember { mutableStateOf(ModuleStatus()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        moduleStatus = ModuleStatusDetector.getModuleStatus(context)
        isLoading = false
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        item {
            if (!isLoading) {
                ModuleStatusCard(moduleStatus = moduleStatus)
            }
        }

        item {
            if (!isLoading) {
                ServiceStatusCard(moduleStatus = moduleStatus)
            }
        }

        item {
            if (!isLoading && moduleStatus.lastError != null) {
                ErrorCard(error = moduleStatus.lastError ?: "")
            }
        }
    }
}

@Composable
fun ModuleStatusCard(moduleStatus: ModuleStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Module Status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                StatusIndicator(
                    isActive = moduleStatus.isXposedActive,
                    activeText = "Active",
                    inactiveText = "Inactive"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (moduleStatus.isXposedActive) {
                    "Successfully activated in LSPosed framework."
                } else {
                    "Module is not activated. Please enable it in LSPosed framework."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ServiceStatusCard(moduleStatus: ModuleStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Service Status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                ServiceStateIndicator(state = moduleStatus.serviceState)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = getServiceStatusDescription(moduleStatus.serviceState),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorCard(error: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun StatusIndicator(
    isActive: Boolean,
    activeText: String,
    inactiveText: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isActive) Color(0xFF4CAF50) else Color(0xFFF44336),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = if (isActive) activeText else inactiveText,
            style = MaterialTheme.typography.labelLarge,
            color = if (isActive) Color(0xFF4CAF50) else Color(0xFFF44336),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ServiceStateIndicator(state: ServiceState) {
    val (icon, color, text) = when (state) {
        ServiceState.READY -> Triple(Icons.Default.CheckCircle, Color(0xFF4CAF50), "Ready")
        ServiceState.STARTING -> Triple(Icons.Default.Info, Color(0xFF2196F3), "Starting")
        ServiceState.ERROR -> Triple(Icons.Default.Warning, Color(0xFFF44336), "Error")
        ServiceState.STOPPING -> Triple(Icons.Default.Warning, Color(0xFFFF9800), "Stopping")
        ServiceState.STOPPED -> Triple(Icons.Default.Warning, Color(0xFF757575), "Stopped")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getServiceStatusDescription(state: ServiceState): String {
    return when (state) {
        ServiceState.READY -> "Packet forwarding service is active and ready to process requests."
        ServiceState.STARTING -> "Service is starting up and initializing components."
        ServiceState.ERROR -> "Service encountered an error and needs attention."
        ServiceState.STOPPING -> "Service is shutting down gracefully."
        ServiceState.STOPPED -> "Service is not running. It will start automatically when needed."
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        Surface {
            HomeScreen()
        }
    }
}
