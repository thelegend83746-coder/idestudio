package com.idestudio.app.data.model

data class BuildLogEntry(
    val stage: String,
    val message: String,
    val isError: Boolean = false,
    val isWarning: Boolean = false,
    val isHeader: Boolean = false,
    val filePath: String? = null,
    val line: Int? = null,
    val column: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class BuildStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    FAILED
}
