package top.wxx9248.lspcarrotjuicer.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import top.wxx9248.lspcarrotjuicer.R
import top.wxx9248.lspcarrotjuicer.ui.screen.HomeScreen
import top.wxx9248.lspcarrotjuicer.ui.screen.LogScreen
import top.wxx9248.lspcarrotjuicer.ui.screen.SettingsScreen
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

// Data class for tab information
data class TabItem(
    val title: Int,
    val icon: ImageVector,
    val content: @Composable (Modifier) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainComponent(modifier: Modifier = Modifier) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // Define tabs with their content
    val tabs = remember {
        listOf(
            TabItem(
                title = R.string.tab_home,
                icon = Icons.Default.Home,
                content = { mod -> HomeScreen(modifier = mod) }
            ),
            TabItem(
                title = R.string.tab_logs,
                icon = Icons.AutoMirrored.Filled.List,
                content = { mod -> LogScreen(modifier = mod) }
            ),
            TabItem(
                title = R.string.tab_settings,
                icon = Icons.Default.Settings,
                content = { mod -> SettingsScreen(modifier = mod) }
            )
        )
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Animation for top app bar title changes
    val titleAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 250,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "titleAlpha"
    )

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
                        text = stringResource(id = tabs[selectedTabIndex].title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(titleAlpha)
                    )
                },
                navigationIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_foreground),
                        contentDescription = stringResource(id = R.string.app_icon_description)
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = stringResource(id = tab.title)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(id = tab.title),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = selectedTabIndex == index,
                        onClick = {
                            if (selectedTabIndex != index) {
                                selectedTabIndex = index
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        val screenModifier = modifier
            .padding(innerPadding)
            .fillMaxSize()

        // Animated content transitions between screens
        AnimatedContent(
            targetState = selectedTabIndex,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = 100,
                        easing = androidx.compose.animation.core.EaseInOut
                    )
                ) togetherWith fadeOut(
                    animationSpec = tween(
                        durationMillis = 100,
                        easing = androidx.compose.animation.core.EaseInOut
                    )
                )
            },
            label = "screenTransition"
        ) { tabIndex ->
            tabs[tabIndex].content(screenModifier)
        }
    }
}

@Preview
@Composable
fun MainComponentPreview() {
    LSPCarrotJuicerTheme {
        MainComponent()
    }
}
