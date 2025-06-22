package top.wxx9248.lspcarrotjuicer.ui.screen


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.wxx9248.lspcarrotjuicer.model.LogEntry
import top.wxx9248.lspcarrotjuicer.model.LogLevel
import top.wxx9248.lspcarrotjuicer.utils.LogManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogScreen(modifier: Modifier = Modifier) {
    LocalContext.current
    val scope = rememberCoroutineScope()

    // State management
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var selectedLogLevel by remember { mutableStateOf<LogLevel?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var isFilterExpanded by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    // Log data
    val logs by LogManager.getAllLogs().collectAsState(initial = emptyList())
    val filteredLogs = remember(logs, searchQuery.text, selectedLogLevel, selectedTag) {
        filterLogs(logs, searchQuery.text, selectedLogLevel, selectedTag)
    }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.scrollToItem(logs.size - 1)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // Search and Filter Controls
        item {
            LogControlsCard(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                selectedLogLevel = selectedLogLevel,
                onLogLevelChange = { selectedLogLevel = it },
                selectedTag = selectedTag,
                onTagChange = { selectedTag = it },
                isFilterExpanded = isFilterExpanded,
                onFilterExpandChange = { isFilterExpanded = it },
                availableTags = logs.map { it.tag }.distinct().sorted(),
                onClearLogs = { showClearDialog = true }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Log Display
        item {
            LogDisplayCard(
                logs = filteredLogs,
                listState = listState
            )
        }
    }

    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Logs") },
            text = { Text("Are you sure you want to clear all logs? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            LogManager.clearLogs()
                        }
                        showClearDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LogControlsCard(
    searchQuery: TextFieldValue,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    selectedLogLevel: LogLevel?,
    onLogLevelChange: (LogLevel?) -> Unit,
    selectedTag: String?,
    onTagChange: (String?) -> Unit,
    isFilterExpanded: Boolean,
    onFilterExpandChange: (Boolean) -> Unit,
    availableTags: List<String>,
    onClearLogs: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Search bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchField(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { onFilterExpandChange(!isFilterExpanded) }) {
                    Icon(
                        imageVector = if (isFilterExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle filters"
                    )
                }

                IconButton(onClick = onClearLogs) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear logs",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Expandable filters
            if (isFilterExpanded) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Log level filter
                    LogLevelFilter(
                        selectedLevel = selectedLogLevel,
                        onLevelSelected = onLogLevelChange
                    )

                    // Tag filter
                    TagFilter(
                        selectedTag = selectedTag,
                        onTagSelected = onTagChange,
                        availableTags = availableTags
                    )
                }
            }
        }
    }
}

@Composable
fun SearchField(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.text.isEmpty()) {
                        Text(
                            text = "Search logs...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
                if (query.text.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange(TextFieldValue("")) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun LogLevelFilter(
    selectedLevel: LogLevel?,
    onLevelSelected: (LogLevel?) -> Unit
) {
    Column {
        Text(
            text = "Log Level",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // "All" chip
            FilterChip(
                onClick = { onLevelSelected(null) },
                label = { Text("All") },
                selected = selectedLevel == null
            )

            // Individual level chips
            LogLevel.entries.forEach { level ->
                FilterChip(
                    onClick = {
                        onLevelSelected(if (selectedLevel == level) null else level)
                    },
                    label = { Text(level.name) },
                    selected = selectedLevel == level,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = getLogLevelColor(level).copy(alpha = 0.2f),
                        selectedLabelColor = getLogLevelColor(level)
                    )
                )
            }
        }
    }
}

@Composable
fun TagFilter(
    selectedTag: String?,
    onTagSelected: (String?) -> Unit,
    availableTags: List<String>
) {
    Column {
        Text(
            text = "Tag",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // "All" chip
            FilterChip(
                onClick = { onTagSelected(null) },
                label = { Text("All") },
                selected = selectedTag == null
            )

            // Tag chips (show first few)
            availableTags.take(3).forEach { tag ->
                FilterChip(
                    onClick = {
                        onTagSelected(if (selectedTag == tag) null else tag)
                    },
                    label = { Text(tag) },
                    selected = selectedTag == tag
                )
            }

            if (availableTags.size > 3) {
                Text(
                    text = "+${availableTags.size - 3} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@Composable
fun LogDisplayCard(
    logs: List<LogEntry>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Logs (${logs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Log entries
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No logs available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { logEntry ->
                        LogEntryItem(logEntry = logEntry)
                    }
                }
            }
        }
    }
}

@Composable
fun LogEntryItem(logEntry: LogEntry) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { isExpanded = !isExpanded }
            .border(
                width = 1.dp,
                color = getLogLevelColor(logEntry.level).copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ),
        color = getLogLevelColor(logEntry.level).copy(alpha = 0.05f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // First line: timestamp, level, tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatTimestamp(logEntry.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = getLogLevelColor(logEntry.level),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = logEntry.level.name.first().toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = logEntry.tag,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Message (truncated or full)
            Text(
                text = if (isExpanded) logEntry.message else logEntry.message.take(120) + if (logEntry.message.length > 120) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Expandable throwable
            if (logEntry.throwable != null && isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Stack trace:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = logEntry.throwable,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Helper functions
private fun filterLogs(
    logs: List<LogEntry>,
    searchQuery: String,
    selectedLevel: LogLevel?,
    selectedTag: String?
): List<LogEntry> {
    return logs.filter { log ->
        val matchesSearch = if (searchQuery.isBlank()) {
            true
        } else {
            log.message.contains(searchQuery, ignoreCase = true) ||
                    log.tag.contains(searchQuery, ignoreCase = true)
        }

        val matchesLevel = selectedLevel == null || log.level == selectedLevel
        val matchesTag = selectedTag == null || log.tag == selectedTag

        matchesSearch && matchesLevel && matchesTag
    }
}

private fun getLogLevelColor(level: LogLevel): Color {
    return when (level) {
        LogLevel.VERBOSE -> Color(0xFF9E9E9E)
        LogLevel.DEBUG -> Color(0xFF2196F3)
        LogLevel.INFO -> Color(0xFF4CAF50)
        LogLevel.WARN -> Color(0xFFFF9800)
        LogLevel.ERROR -> Color(0xFFF44336)
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@Preview
@Composable
fun LogScreenPreview() {
    MaterialTheme {
        Surface {
            LogScreen()
        }
    }
}
