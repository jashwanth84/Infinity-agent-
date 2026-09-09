package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppMode
import com.example.data.model.WorkspaceTab
import com.example.ui.MainViewModel
import com.example.ui.components.DiffViewerDialog
import com.example.ui.components.LeftSlidingDrawerContent
import com.example.ui.components.ModelSelectorSheet
import com.example.ui.screens.*
import com.example.ui.theme.AccentCyanBright
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentMode by viewModel.currentMode.collectAsState()
    val currentWorkspaceTab by viewModel.currentWorkspaceTab.collectAsState()
    val currentModel by viewModel.currentModel.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val activeDiff by viewModel.activeDiff.collectAsState()

    var showModelSelectorSheet by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            LeftSlidingDrawerContent(
                currentMode = currentMode,
                currentWorkspaceTab = currentWorkspaceTab,
                isDarkMode = isDarkMode,
                onModeSelected = { mode ->
                    viewModel.setMode(mode)
                },
                onWorkspaceTabSelected = { tab ->
                    viewModel.setWorkspaceTab(tab)
                },
                onToggleTheme = {
                    viewModel.toggleDarkMode()
                },
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when {
                                    currentWorkspaceTab != null -> currentWorkspaceTab!!.title
                                    else -> currentMode.title
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        // Fast Model Switch Button
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showModelSelectorSheet = true }
                                .padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AccentCyanBright)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentModel.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.toggleDarkMode() }) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Toggle Dark/Light",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.systemBars
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Crossfade(
                    targetState = Pair(currentMode, currentWorkspaceTab),
                    label = "ScreenTransition"
                ) { (mode, workspaceTab) ->
                    when {
                        workspaceTab == WorkspaceTab.PROJECTS -> {
                            ProjectsScreen(viewModel = viewModel)
                        }
                        workspaceTab == WorkspaceTab.FILES -> {
                            FilesScreen(viewModel = viewModel)
                        }
                        workspaceTab == WorkspaceTab.HISTORY -> {
                            HistoryScreen(viewModel = viewModel)
                        }
                        mode == AppMode.CHAT -> {
                            ChatScreen(
                                viewModel = viewModel,
                                onOpenModelSelector = { showModelSelectorSheet = true }
                            )
                        }
                        mode == AppMode.CODER -> {
                            CoderScreen(viewModel = viewModel)
                        }
                        mode == AppMode.AGENT -> {
                            AgentScreen(viewModel = viewModel)
                        }
                        mode == AppMode.MULTI_AGENT -> {
                            MultiAgentScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    // Model Selector Bottom Sheet
    if (showModelSelectorSheet) {
        ModelSelectorSheet(
            currentModel = currentModel,
            onModelSelected = { model ->
                viewModel.setModel(model)
            },
            onDismiss = { showModelSelectorSheet = false }
        )
    }

    // Diff Viewer Dialog (Apply / Reject changes)
    if (activeDiff != null) {
        val diff = activeDiff!!
        DiffViewerDialog(
            filePath = diff.filePath,
            oldContent = diff.originalContent,
            newContent = diff.proposedContent,
            onApply = { viewModel.applyDiffToProject(diff) },
            onReject = { viewModel.dismissDiff() }
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
