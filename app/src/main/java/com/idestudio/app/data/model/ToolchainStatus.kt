package com.idestudio.app.data.model

data class ToolComponent(
    val name: String,
    val isAvailable: Boolean,
    val version: String,
    val path: String,
    val description: String,
    val isRequired: Boolean = true
)

data class ToolchainStatus(
    val isReadyToBuild: Boolean,
    val components: List<ToolComponent>,
    val androidJarPath: String?,
    val storageUsedBytes: Long = 0L,
    val summary: String = ""
)
