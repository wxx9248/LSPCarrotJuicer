package top.wxx9248.lspcarrotjuicer.ui.composable

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun LogScreen(modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
    }
}

@Preview
@Composable
fun LogScreenPreview() {
    LogScreen()
}
