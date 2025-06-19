package top.wxx9248.lspcarrotjuicer.ui.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        item() {StatusCard()}
    }
}

@Composable
fun StatusCard() {
    Card(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val isActivated =
                remember { mutableStateOf(false) } // This should come from actual module status
            Text(text = if (isActivated.value) "Module is Activated" else "Module is Deactivated")
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}
