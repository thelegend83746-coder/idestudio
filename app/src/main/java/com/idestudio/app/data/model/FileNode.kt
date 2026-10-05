package com.idestudio.app.data.model

import java.io.File

data class FileNode(
    val file: File,
    val isDirectory: Boolean,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val extension: String = file.extension.lowercase(),
    val children: List<FileNode> = emptyList(),
    val isExpanded: Boolean = false,
    val depth: Int = 0
) {
    val isJava: Boolean get() = extension == "java"
    val isXml: Boolean get() = extension == "xml"
    val isGradle: Boolean get() = extension == "gradle" || name.endsWith(".gradle.kts")
    val isJson: Boolean get() = extension == "json"
    val isImage: Boolean get() = extension in listOf("png", "jpg", "jpeg", "webp", "gif")
    val isEditable: Boolean get() = isJava || isXml || isGradle || isJson || extension in listOf("txt", "md", "properties", "pro")
}
