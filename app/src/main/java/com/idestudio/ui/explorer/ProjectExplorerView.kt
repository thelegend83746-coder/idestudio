package com.idestudio.ui.explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.idestudio.data.model.FileNode
import com.idestudio.ui.theme.*
import java.io.File

@Composable
fun ProjectExplorerView(
    rootNode: FileNode?,
    activeFilePath: String?,
    onFileSelected: (File) -> Unit,
    onRefreshTree: () -> Unit,
    onCreateFile: (parentDir: File, fileName: String) -> Unit,
    onCreateFolder: (parentDir: File, folderName: String) -> Unit,
    onRenameFile: (target: File, newName: String) -> Unit,
    onDeleteFile: (target: File) -> Unit
) {
    var expandedNodes by remember { mutableStateOf(setOf<String>()) }
    var showCreateFileDialog by remember { mutableStateOf<File?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf<File?>(null) }
    var showRenameDialog by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<File?>(null) }

    var inputName by remember { mutableStateOf("") }

    // Auto-expand root
    LaunchedEffect(rootNode) {
        rootNode?.let {
            expandedNodes = expandedNodes + it.path
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface)
    ) {
        // Explorer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "PROJECT FILES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextTertiary
            )

            Row {
                IconButton(
                    onClick = {
                        rootNode?.let {
                            inputName = ""
                            showCreateFileDialog = it.file
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.NoteAdd, contentDescription = "Add File", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = {
                        rootNode?.let {
                            inputName = ""
                            showCreateFolderDialog = it.file
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Folder", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onRefreshTree,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        Divider(color = DarkBorder)

        // Tree List
        if (rootNode != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item {
                    RenderNodeRecursively(
                        node = rootNode,
                        depth = 0,
                        expandedNodes = expandedNodes,
                        activeFilePath = activeFilePath,
                        onToggleExpand = { targetNode ->
                            expandedNodes = if (expandedNodes.contains(targetNode.path)) {
                                expandedNodes - targetNode.path
                            } else {
                                expandedNodes + targetNode.path
                            }
                        },
                        onFileClick = { onFileSelected(it.file) },
                        onNewFile = {
                            inputName = ""
                            showCreateFileDialog = it.file
                        },
                        onNewFolder = {
                            inputName = ""
                            showCreateFolderDialog = it.file
                        },
                        onRename = {
                            inputName = it.name
                            showRenameDialog = it.file
                        },
                        onDelete = { showDeleteConfirmDialog = it.file }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandPrimary, modifier = Modifier.size(24.dp))
            }
        }
    }

    // Create File Dialog
    showCreateFileDialog?.let { parent ->
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = null },
            title = { Text("New File", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("Filename (e.g. Utils.java)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            onCreateFile(parent, inputName.trim())
                            showCreateFileDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateFileDialog = null }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // Create Folder Dialog
    showCreateFolderDialog?.let { parent ->
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = null },
            title = { Text("New Folder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            onCreateFolder(parent, inputName.trim())
                            showCreateFolderDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateFolderDialog = null }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // Rename Dialog
    showRenameDialog?.let { target ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            onRenameFile(target, inputName.trim())
                            showRenameDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRenameDialog = null }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // Delete Confirmation Dialog
    showDeleteConfirmDialog?.let { target ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete '${target.name}'?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this ${if (target.isDirectory) "folder and all its contents" else "file"}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFile(target)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}

@Composable
private fun RenderNodeRecursively(
    node: FileNode,
    depth: Int,
    expandedNodes: Set<String>,
    activeFilePath: String?,
    onToggleExpand: (FileNode) -> Unit,
    onFileClick: (FileNode) -> Unit,
    onNewFile: (FileNode) -> Unit,
    onNewFolder: (FileNode) -> Unit,
    onRename: (FileNode) -> Unit,
    onDelete: (FileNode) -> Unit
) {
    val isExpanded = expandedNodes.contains(node.path)

    FileTreeItem(
        node = node.copy(isExpanded = isExpanded),
        depth = depth,
        activeFilePath = activeFilePath,
        onFileClick = onFileClick,
        onToggleExpand = onToggleExpand,
        onNewFile = onNewFile,
        onNewFolder = onNewFolder,
        onRename = onRename,
        onDelete = onDelete
    )

    if (node.isDirectory && isExpanded) {
        for (child in node.children) {
            RenderNodeRecursively(
                node = child,
                depth = depth + 1,
                expandedNodes = expandedNodes,
                activeFilePath = activeFilePath,
                onToggleExpand = onToggleExpand,
                onFileClick = onFileClick,
                onNewFile = onNewFile,
                onNewFolder = onNewFolder,
                onRename = onRename,
                onDelete = onDelete
            )
        }
    }
}
