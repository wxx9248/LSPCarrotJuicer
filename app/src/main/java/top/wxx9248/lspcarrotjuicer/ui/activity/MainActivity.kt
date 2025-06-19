package top.wxx9248.lspcarrotjuicer.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import top.wxx9248.lspcarrotjuicer.R
import top.wxx9248.lspcarrotjuicer.ui.composable.HomeScreen
import top.wxx9248.lspcarrotjuicer.ui.composable.LogScreen
import top.wxx9248.lspcarrotjuicer.ui.composable.SettingsScreen
import top.wxx9248.lspcarrotjuicer.ui.theme.LSPCarrotJuicerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LSPCarrotJuicerTheme {
                MainComponent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainComponent(modifier: Modifier = Modifier) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val tabStringResources = listOf(
        R.string.tab_home,
        R.string.tab_settings,
        R.string.tab_logs
    )

    val tabStrings = tabStringResources.map {
        stringResource(id = it)
    }

    val tabIcons = tabStringResources.map {
        when (it) {
            R.string.tab_home -> Icons.Default.Home
            R.string.tab_settings -> Icons.Default.Settings
            R.string.tab_logs -> Icons.AutoMirrored.Filled.List
            else -> throw NotImplementedError("Icon not assigned for all tabStringResources")
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Text(
                        tabStrings[selectedTabIndex],
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_foreground),
                            contentDescription = stringResource(id = R.string.app_icon_description)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            NavigationBar {
                tabStringResources.forEachIndexed { index, _ ->
                    NavigationBarItem(
                        icon = { Icon(tabIcons[index], contentDescription = tabStrings[index]) },
                        label = { Text(tabStrings[index]) },
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        val screenModifier = modifier
            .padding(innerPadding)
            .padding(horizontal = 16.dp)
            .fillMaxSize()
        when (selectedTabIndex) {
            0 -> HomeScreen(modifier = screenModifier)
            1 -> SettingsScreen(modifier = screenModifier)
            2 -> LogScreen(modifier = screenModifier)
            else -> throw IndexOutOfBoundsException("selectedTabIndex out of bound")
        }
    }
}

@Preview
@Composable
fun MainComponentPreview() {
    MainComponent()
}
