package com.idestudio.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idestudio.ui.ai.AiAssistantDialog
import com.idestudio.ui.buildlog.BuildLogPanel
import com.idestudio.ui.explorer.ProjectExplorerView
import com.idestudio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    projectId: String,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val editorSettings by viewModel.editorSettings.collectAsState()
    val buildState by viewModel.buildState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(projectId) {
        viewModel.initProject(projectId)
    }

    LaunchedEffect(uiState.editorMessage) {
        uiState.editorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val isJava = uiState.activeFile?.extension?.equals("java", ignoreCase = true) == true
    val isXml = uiState.activeFile?.extension?.equals("xml", ignoreCase = true) == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.activeFile?.name ?: uiState.project?.name ?: "Editor",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        uiState.project?.let {
                            Text(
                                text = it.name,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        IconButton(onClick = { viewModel.toggleExplorer() }) {
                            Icon(
                                imageVector = if (uiState.isExplorerOpen) Icons.Default.FolderOpen else Icons.Default.Folder,
                                contentDescription = "Project Tree",
                                tint = if (uiState.isExplorerOpen) BrandPrimary else TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    // Undo
                    IconButton(onClick = { viewModel.undo() }) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                    // Redo
                    IconButton(onClick = { viewModel.redo() }) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                    // Save
                    IconButton(onClick = { viewModel.saveActiveFile() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                    // Build AI
                    IconButton(onClick = { viewModel.openAiAssistant() }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Build AI", tint = BrandAccentAi, modifier = Modifier.size(20.dp))
                    }
                    // Build APK
                    IconButton(onClick = { viewModel.startBuild() }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Build APK", tint = StatusSuccess, modifier = Modifier.size(24.dp))
                    }
                    // Settings
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Open Tabs row
                FileTabs(
                    tabs = uiState.openTabs,
                    activeFile = uiState.activeFile,
                    onSelectTab = { viewModel.openFile(it) },
                    onCloseTab = { viewModel.closeTab(it) }
                )

                // Main editor canvas
                if (uiState.activeFile != null) {
                    Box(modifier = Modifier.weight(1f)) {
                        CodeEditorSurface(
                            textFieldValue = uiState.activeContent,
                            onValueChange = { viewModel.onContentChanged(it) },
                            settings = editorSettings,
                            isJava = isJava,
                            isXml = isXml,
                            errorLine = uiState.targetErrorLine,
                            onInsertSymbol = { viewModel.insertSymbol(it) }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No file open", color = TextSecondary, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(onClick = { viewModel.toggleExplorer() }) {
                                Text("Open Project Files", color = BrandPrimary)
                            }
                        }
                    }
                }

                // Expandable Build Log Panel
                BuildLogPanel(
                    buildState = buildState,
                    isExpanded = uiState.isBuildLogExpanded,
                    onToggleExpand = { viewModel.toggleBuildLog() },
                    onNavigateToError = { file, line -> viewModel.navigateToDiagnostic(file, line) },
                    onCancelBuild = { viewModel.cancelBuild() }
                )
            }

            // Project Explorer Drawer Overlay
            if (uiState.isExplorerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(280.dp)
                        .background(DarkSurface)
                        .align(Alignment.CenterStart)
                ) {
                    ProjectExplorerView(
                        rootNode = uiState.rootFileNode,
                        activeFilePath = uiState.activeFile?.absolutePath,
                        onFileSelected = { viewModel.openFile(it) },
                        onRefreshTree = { uiState.project?.let { viewModel.loadProjectTree(it) } },
                        onCreateFile = { parent, name -> viewModel.createFileInProject(parent, name) },
                        onCreateFolder = { parent, name -> viewModel.createFolderInProject(parent, name) },
                        onRenameFile = { target, newName -> viewModel.renameFileInProject(target, newName) },
                        onDeleteFile = { target -> viewModel.deleteFileInProject(target) }
                    )
                }
            }

            // AI Assistant Dialog
            if (uiState.isAiAssistantOpen) {
                AiAssistantDialog(
                    activeFile = uiState.activeFile,
                    isGenerating = uiState.isAiGenerating,
                    generationError = uiState.aiGenerationError,
                    proposals = uiState.aiProposals,
                    onDismiss = { viewModel.closeAiAssistant() },
                    onRequestChange = { prompt -> viewModel.requestAiAssistance(prompt) },
                    onApplyProposals = { props -> viewModel.applyAiProposals(props) }
                )
            }
        }
    }
}
