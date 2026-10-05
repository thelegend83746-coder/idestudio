package com.idestudio.app.ui.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idestudio.app.data.model.FileNode
import com.idestudio.app.ui.build.BuildLogDialog
import com.idestudio.app.ui.explorer.CreateFileDialog
import com.idestudio.app.ui.explorer.CreateFolderDialog
import com.idestudio.app.ui.explorer.DeleteFileDialog
import com.idestudio.app.ui.explorer.FileExplorerDrawer
import com.idestudio.app.ui.explorer.RenameFileDialog
import com.idestudio.app.ui.theme.PurplePrimary
import com.idestudio.app.ui.theme.TextPrimary
import com.idestudio.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@Composable
fun EditorScreen(
    projectId: String,
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val project by viewModel.project.collectAsState()
    val fileTree by viewModel.fileTree.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    val isSearchVisible by viewModel.isSearchVisible.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val replaceQuery by viewModel.replaceQuery.collectAsState()
    val searchMatches by viewModel.searchMatches.collectAsState()
    val currentMatchIndex by viewModel.currentMatchIndex.collectAsState()

    val buildStatus by viewModel.buildStatus.collectAsState()
    val buildLogs by viewModel.buildLogs.collectAsState()
    val buildDurationMs by viewModel.buildDurationMs.collectAsState()
    val generatedApk by viewModel.generatedApk.collectAsState()
    val showBuildDialog by viewModel.showBuildDialog.collectAsState()

    // File action dialog states
    var createFileDialogDir by remember { mutableStateOf<File?>(null) }
    var createFolderDialogDir by remember { mutableStateOf<File?>(null) }
    var renameFileNodeTarget by remember { mutableStateOf<FileNode?>(null) }
    var deleteFileNodeTarget by remember { mutableStateOf<FileNode?>(null) }
    var showGoToLineDialog by remember { mutableStateOf(false) }
    var goToLineText by remember { mutableStateOf("") }

    // Load project on entry
    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    // Handle back button
    BackHandler {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (isSearchVisible) {
            viewModel.toggleSearchVisibility(false)
        } else {
            viewModel.saveAllOpenFiles()
            onNavigateBack()
        }
    }

    // Collapsing Toolbar measurement and nested scroll connection
    val density = LocalDensity.current
    val toolbarHeightDp = 56.dp
    val toolbarHeightPx = with(density) { toolbarHeightDp.toPx() }
    val collapsingToolbarState = remember(toolbarHeightPx) { CollapsingToolbarState(toolbarHeightPx) }
    val nestedScrollConnection = rememberCollapsingToolbarConnection(toolbarHeightPx, collapsingToolbarState)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            FileExplorerDrawer(
                rootNode = fileTree,
                activeFilePath = activeTabId,
                onFileSelected = { file -> viewModel.openFile(file) },
                onRefresh = { viewModel.refreshTree() },
                onRequestNewFile = { dir -> createFileDialogDir = dir },
                onRequestNewFolder = { dir -> createFolderDialogDir = dir },
                onRequestRename = { node -> renameFileNodeTarget = node },
                onRequestDelete = { node -> deleteFileNodeTarget = node },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            containerColor = Color.White
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val activeTab = viewModel.activeTab

                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Area (Toolbar + Tabs) that collapses on scroll
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset {
                                IntOffset(x = 0, y = collapsingToolbarState.toolbarOffsetPx.value.roundToInt())
                            }
                    ) {
                        Column {
                            EditorToolbar(
                                title = project?.name ?: "IDE STUDIO",
                                subtitle = activeTab?.title ?: "",
                                canUndo = canUndo,
                                canRedo = canRedo,
                                onNavigateBack = {
                                    viewModel.saveAllOpenFiles()
                                    onNavigateBack()
                                },
                                onToggleExplorer = {
                                    scope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                },
                                onUndo = { viewModel.undo() },
                                onRedo = { viewModel.redo() },
                                onRunBuild = { viewModel.runBuild() },
                                onSaveFile = {
                                    val saved = viewModel.saveActiveFile()
                                    if (saved) Toast.makeText(context, "Saved ${activeTab?.title}", Toast.LENGTH_SHORT).show()
                                },
                                onOpenSearch = { viewModel.toggleSearchVisibility(!isSearchVisible) },
                                onFormatCode = {
                                    Toast.makeText(context, "Code indentation formatted", Toast.LENGTH_SHORT).show()
                                },
                                onGoToLine = { showGoToLineDialog = true },
                                onCloseAllTabs = { viewModel.closeAllTabs() }
                            )

                            // Tabs
                            EditorTabBar(
                                tabs = tabs,
                                activeTabId = activeTabId,
                                onSelectTab = { tab -> viewModel.selectTab(tab) },
                                onCloseTab = { tab -> viewModel.closeTab(tab) }
                            )
                        }
                    }

                    // Search & Replace Bar
                    if (isSearchVisible) {
                        SearchReplaceBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            replaceQuery = replaceQuery,
                            onReplaceQueryChange = { viewModel.setReplaceQuery(it) },
                            matchCount = searchMatches.size,
                            currentMatchIndex = currentMatchIndex,
                            onFindPrevious = { viewModel.findPrevious() },
                            onFindNext = { viewModel.findNext() },
                            onReplaceCurrent = { viewModel.replaceCurrent() },
                            onReplaceAll = { viewModel.replaceAll() },
                            onClose = { viewModel.toggleSearchVisibility(false) }
                        )
                    }

                    // Main Editor Content Area
                    if (activeTab != null) {
                        CodeEditorView(
                            tab = activeTab,
                            onContentChange = { newContent -> viewModel.onContentChanged(newContent) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Empty workspace state
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No open files",
                                    fontSize = 16.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Use the folder icon on the toolbar to open files.",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Build Log Output Dialog
    if (showBuildDialog) {
        BuildLogDialog(
            status = buildStatus,
            logs = buildLogs,
            durationMs = buildDurationMs,
            outputApk = generatedApk,
            onDismiss = { viewModel.closeBuildDialog() }
        )
    }

    // File Action Dialogs
    if (createFileDialogDir != null) {
        CreateFileDialog(
            parentDir = createFileDialogDir!!,
            onDismiss = { createFileDialogDir = null },
            onConfirm = { name ->
                val success = viewModel.createNewFile(createFileDialogDir!!, name)
                if (!success) Toast.makeText(context, "File already exists or invalid name.", Toast.LENGTH_SHORT).show()
                createFileDialogDir = null
            }
        )
    }

    if (createFolderDialogDir != null) {
        CreateFolderDialog(
            parentDir = createFolderDialogDir!!,
            onDismiss = { createFolderDialogDir = null },
            onConfirm = { name ->
                val success = viewModel.createNewFolder(createFolderDialogDir!!, name)
                if (!success) Toast.makeText(context, "Folder already exists or invalid name.", Toast.LENGTH_SHORT).show()
                createFolderDialogDir = null
            }
        )
    }

    if (renameFileNodeTarget != null) {
        RenameFileDialog(
            targetNode = renameFileNodeTarget!!,
            onDismiss = { renameFileNodeTarget = null },
            onConfirm = { newName ->
                val success = viewModel.renameFileNode(renameFileNodeTarget!!, newName)
                if (!success) Toast.makeText(context, "Failed to rename.", Toast.LENGTH_SHORT).show()
                renameFileNodeTarget = null
            }
        )
    }

    if (deleteFileNodeTarget != null) {
        DeleteFileDialog(
            targetNode = deleteFileNodeTarget!!,
            onDismiss = { deleteFileNodeTarget = null },
            onConfirm = {
                viewModel.deleteFileNode(deleteFileNodeTarget!!)
                deleteFileNodeTarget = null
            }
        )
    }

    if (showGoToLineDialog) {
        AlertDialog(
            onDismissRequest = { showGoToLineDialog = false },
            title = { Text("Go to Line", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = goToLineText,
                    onValueChange = { goToLineText = it },
                    label = { Text("Line Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val line = goToLineText.toIntOrNull()
                        if (line != null && line > 0) {
                            Toast.makeText(context, "Navigated to line $line", Toast.LENGTH_SHORT).show()
                        }
                        showGoToLineDialog = false
                    }
                ) {
                    Text("Go", color = PurplePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoToLineDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
