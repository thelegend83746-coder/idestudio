package com.idestudio.app.data.model

import java.io.File

data class EditorTab(
    val id: String = file.absolutePath,
    val file: File,
    val title: String = file.name,
    var content: String = "",
    var originalContent: String = "",
    var isModified: Boolean = false,
    var cursorPosition: Int = 0,
    var scrollPosition: Int = 0
) {
    val isJava: Boolean get() = file.extension.equals("java", ignoreCase = true)
    val isXml: Boolean get() = file.extension.equals("xml", ignoreCase = true)
}
