package top.wxx9248.lspcarrotjuicer.ui.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        item { ItemEnableModule() }
        item { Spacer(modifier = Modifier.padding(vertical = 16.dp)) }
        item { ItemUrlInputBox() }
    }
}

@Composable
fun ItemEnableModule(modifier: Modifier = Modifier) {
    var isModuleEnabled by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text("Enable Module")
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = isModuleEnabled,
            onCheckedChange = { isModuleEnabled = it }
        )
    }
}

@Composable
fun ItemUrlInputBox(modifier: Modifier = Modifier) {
    var url by remember { mutableStateOf(TextFieldValue("")) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URA Server Address") },
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(vertical = 8.dp))
        Button(
            onClick = {},
            modifier = Modifier.align(androidx.compose.ui.Alignment.End)
        ) {
            Text("Test Availability")
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
