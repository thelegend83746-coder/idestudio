package com.idestudio.data.model

enum class BuildStatus {
    IDLE,
    PREPARING,
    PROCESSING_RESOURCES,
    COMPILING_JAVA,
    CONVERTING_BYTECODE,
    PACKAGING,
    SIGNING,
    VERIFYING,
    SUCCESS,
    FAILURE,
    CANCELLED
}

enum class LogLevel {
    INFO,
    WARNING,
    ERROR
}

data class Diagnostic(
    val filePath: String?,
    val lineNumber: Int?,
    val columnNumber: Int? = null,
    val message: String,
    val isError: Boolean = true
)

data class BuildLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val stage: BuildStatus,
    val message: String,
    val diagnostic: Diagnostic? = null
)

data class BuildState(
    val status: BuildStatus = BuildStatus.IDLE,
    val stageMessage: String = "Ready to build",
    val logs: List<BuildLogEntry> = emptyList(),
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L,
    val outputApkPath: String? = null,
    val apkSizeBytes: Long = 0L,
    val errorDiagnostics: List<Diagnostic> = emptyList()
) {
    val durationMs: Long
        get() = if (startTimeMs == 0L) 0L else (if (endTimeMs > 0L) endTimeMs - startTimeMs else System.currentTimeMillis() - startTimeMs)

    val isRunning: Boolean
        get() = status in listOf(
            BuildStatus.PREPARING,
            BuildStatus.PROCESSING_RESOURCES,
            BuildStatus.COMPILING_JAVA,
            BuildStatus.CONVERTING_BYTECODE,
            BuildStatus.PACKAGING,
            BuildStatus.SIGNING,
            BuildStatus.VERIFYING
        )
}
