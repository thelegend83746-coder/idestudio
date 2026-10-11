package com.idestudio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: String,
    val name: String,
    val packageName: String,
    val rootPath: String,
    val minSdk: Int = 21,
    val targetSdk: Int = 34,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val templateId: String = "empty_activity",
    val language: String = "Java",
    val isFavorited: Boolean = false
) {
    val srcDir: String
        get() = "$rootPath/app/src/main/java/${packageName.replace('.', '/')}"

    val resDir: String
        get() = "$rootPath/app/src/main/res"

    val manifestPath: String
        get() = "$rootPath/app/src/main/AndroidManifest.xml"

    val buildDir: String
        get() = "$rootPath/build"

    val outputApkPath: String
        get() = "$buildDir/outputs/app-debug.apk"
}
