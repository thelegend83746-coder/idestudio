package com.idestudio.data.model

import java.io.File

data class FileNode(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val isExpanded: Boolean = false,
    val children: List<FileNode> = emptyList(),
    val sizeBytes: Long = if (file.isFile) file.length() else 0L,
    val lastModified: Long = file.lastModified()
) {
    val extension: String
        get() = file.extension.lowercase()

    val isJava: Boolean
        get() = extension == "java"

    val isXml: Boolean
        get() = extension == "xml"

    val isGradle: Boolean
        get() = extension == "gradle" || extension == "kts"

    val isImage: Boolean
        get() = extension in listOf("png", "jpg", "jpeg", "webp", "gif")

    val isManifest: Boolean
        get() = name.equals("AndroidManifest.xml", ignoreCase = true)
}
