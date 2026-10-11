package com.idestudio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class EditorSettings(
    val fontSizeSp: Float = 17f,
    val tabSize: Int = 4,
    val wordWrap: Boolean = false,
    val showLineNumbers: Boolean = true,
    val highlightCurrentLine: Boolean = true,
    val autoComplete: Boolean = true,
    val isDarkTheme: Boolean = true
)
