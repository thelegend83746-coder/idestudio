package com.idestudio.app.util

import com.idestudio.app.data.model.FileNode
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    fun buildFileTree(directory: File, depth: Int = 0): FileNode {
        if (!directory.exists()) {
            return FileNode(
                file = directory,
                isDirectory = true,
                children = emptyList(),
                depth = depth
            )
        }

        val children = if (directory.isDirectory) {
            directory.listFiles()?.map { child ->
                if (child.isDirectory) {
                    buildFileTree(child, depth + 1)
                } else {
                    FileNode(
                        file = child,
                        isDirectory = false,
                        depth = depth + 1
                    )
                }
            }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) }))
                ?: emptyList()
        } else {
            emptyList()
        }

        return FileNode(
            file = directory,
            isDirectory = directory.isDirectory,
            children = children,
            isExpanded = depth < 2, // Auto expand root and first level
            depth = depth
        )
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
