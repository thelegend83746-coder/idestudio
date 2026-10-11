package com.idestudio.ui.explorer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idestudio.data.model.FileNode
import com.idestudio.ui.theme.*

@Composable
fun FileTreeItem(
    node: FileNode,
    depth: Int = 0,
    activeFilePath: String?,
    onFileClick: (FileNode) -> Unit,
    onToggleExpand: (FileNode) -> Unit,
    onNewFile: (FileNode) -> Unit,
    onNewFolder: (FileNode) -> Unit,
    onRename: (FileNode) -> Unit,
    onDelete: (FileNode) -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    val isSelected = node.file.absolutePath == activeFilePath

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (node.isDirectory) {
                        onToggleExpand(node)
                    } else {
                        onFileClick(node)
                    }
                }
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .padding(start = (depth * 16).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            if (node.isDirectory) {
                Icon(
                    imageVector = if (node.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = when {
                        node.isJava -> Icons.Default.Code
                        node.isXml -> Icons.Default.DataObject
                        node.isImage -> Icons.Default.Image
                        node.isManifest -> Icons.Default.Android
                        else -> Icons.Default.Description
                    },
                    contentDescription = null,
                    tint = when {
                        node.isJava -> SyntaxType
                        node.isXml -> SyntaxXmlTag
                        node.isManifest -> BrandSecondary
                        else -> TextSecondary
                    },
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = node.name,
                fontSize = 13.sp,
                color = if (isSelected) BrandPrimary else TextPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            // Context Action Menu
            Box {
                IconButton(
                    onClick = { expandedMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    if (node.isDirectory) {
                        DropdownMenuItem(
                            text = { Text("New File") },
                            onClick = {
                                expandedMenu = false
                                onNewFile(node)
                            },
                            leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("New Folder") },
                            onClick = {
                                expandedMenu = false
                                onNewFolder(node)
                            },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) }
                        )
                        Divider()
                    }
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            expandedMenu = false
                            onRename(node)
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = StatusError) },
                        onClick = {
                            expandedMenu = false
                            onDelete(node)
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusError) }
                    )
                }
            }
        }

        // Render Children if Expanded
        if (node.isDirectory && node.isExpanded) {
            for (child in node.children) {
                FileTreeItem(
                    node = child,
                    depth = depth + 1,
                    activeFilePath = activeFilePath,
                    onFileClick = onFileClick,
                    onToggleExpand = onToggleExpand,
                    onNewFile = onNewFile,
                    onNewFolder = onNewFolder,
                    onRename = onRename,
                    onDelete = onDelete
                )
            }
        }
    }
}
