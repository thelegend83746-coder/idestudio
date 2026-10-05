package com.idestudio.app.ui.explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idestudio.app.data.model.FileNode
import com.idestudio.app.ui.theme.PurplePrimary
import com.idestudio.app.ui.theme.TextPrimary
import com.idestudio.app.ui.theme.TextSecondary
import java.io.File

@Composable
fun FileTreeView(
    rootNode: FileNode,
    activeFilePath: String?,
    onFileSelected: (File) -> Unit,
    onRequestNewFile: (File) -> Unit,
    onRequestNewFolder: (File) -> Unit,
    onRequestRename: (FileNode) -> Unit,
    onRequestDelete: (FileNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    Column(modifier = modifier.fillMaxWidth()) {
        FileTreeNodeItem(
            node = rootNode,
            depth = 0,
            activeFilePath = activeFilePath,
            expandedStates = expandedStates,
            onFileSelected = onFileSelected,
            onRequestNewFile = onRequestNewFile,
            onRequestNewFolder = onRequestNewFolder,
            onRequestRename = onRequestRename,
            onRequestDelete = onRequestDelete
        )
    }
}

@Composable
fun FileTreeNodeItem(
    node: FileNode,
    depth: Int,
    activeFilePath: String?,
    expandedStates: MutableMap<String, Boolean>,
    onFileSelected: (File) -> Unit,
    onRequestNewFile: (File) -> Unit,
    onRequestNewFolder: (File) -> Unit,
    onRequestRename: (FileNode) -> Unit,
    onRequestDelete: (FileNode) -> Unit
) {
    val isExpanded = expandedStates[node.path] ?: (depth < 2)
    val isActive = activeFilePath == node.path
    var menuOpen by remember { mutableStateOf(false) }

    val bgColor = when {
        isActive -> Color(0xFFEDE7F6)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, shape = RoundedCornerShape(4.dp))
            .clickable {
                if (node.isDirectory) {
                    expandedStates[node.path] = !isExpanded
                } else {
                    onFileSelected(node.file)
                }
            }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indentation spacer
        Spacer(modifier = Modifier.width((depth * 14).dp))

        // Arrow expander for folders
        if (node.isDirectory) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(18.dp))
        }

        Spacer(modifier = Modifier.width(4.dp))

        // File / Folder Icon
        val (icon, tint) = getIconForNode(node, isExpanded)
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // File name text
        Text(
            text = node.name,
            fontSize = 13.sp,
            fontWeight = if (isActive || node.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) PurplePrimary else TextPrimary,
            modifier = Modifier.weight(1f)
        )

        // Options menu
        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                if (node.isDirectory) {
                    DropdownMenuItem(
                        text = { Text("New File") },
                        onClick = {
                            menuOpen = false
                            onRequestNewFile(node.file)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New Folder") },
                        onClick = {
                            menuOpen = false
                            onRequestNewFolder(node.file)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        menuOpen = false
                        onRequestRename(node)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color.Red) },
                    onClick = {
                        menuOpen = false
                        onRequestDelete(node)
                    }
                )
            }
        }
    }

    // Render children recursively if expanded
    if (node.isDirectory && isExpanded) {
        node.children.forEach { child ->
            FileTreeNodeItem(
                node = child,
                depth = depth + 1,
                activeFilePath = activeFilePath,
                expandedStates = expandedStates,
                onFileSelected = onFileSelected,
                onRequestNewFile = onRequestNewFile,
                onRequestNewFolder = onRequestNewFolder,
                onRequestRename = onRequestRename,
                onRequestDelete = onRequestDelete
            )
        }
    }
}

private fun getIconForNode(node: FileNode, isExpanded: Boolean): Pair<ImageVector, Color> {
    return when {
        node.isDirectory -> {
            if (isExpanded) {
                Icons.Default.FolderOpen to Color(0xFFFFA000)
            } else {
                Icons.Default.Folder to Color(0xFFFFA000)
            }
        }
        node.isJava -> Icons.Default.Code to Color(0xFF1E88E5)
        node.isXml -> Icons.AutoMirrored.Filled.InsertDriveFile to Color(0xFFFB8C00)
        node.isGradle -> Icons.Default.Code to Color(0xFF43A047)
        node.isImage -> Icons.Default.Image to Color(0xFF8E24AA)
        else -> Icons.AutoMirrored.Filled.InsertDriveFile to Color(0xFF757575)
    }
}
