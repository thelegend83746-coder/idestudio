package com.idestudio.engine

import android.content.Context
import com.idestudio.data.model.SdkPlatformInfo
import com.idestudio.data.model.SdkStatus
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class SdkPlatformManager(private val context: Context) {

    // Common search directories for Android SDK platform files
    private val candidateSdkDirs = listOf(
        File("/storage/emulated/0/test-folder/ide-studio/app/src/main/assets/platform"),
        File("/storage/emulated/0/test-folder/ide-studio/app/src/main/assets/platforms"),
        File("/storage/emulated/0/test-folder/android-sdk/platforms"),
        File("/storage/emulated/0/test-folder/android-sdk"),
        File("/storage/emulated/0/test-folder/ide-studio/app/libs"),
        File(context.filesDir, "platform"),
        File(context.filesDir, "platforms"),
        File(context.filesDir, "toolchain"),
        File(context.getExternalFilesDir(null), "sdk"),
        File("/system/framework")
    )

    fun getPlatformInfoList(): List<SdkPlatformInfo> {
        return SdkPlatformInfo.ALL_KNOWN_PLATFORMS.map { baseInfo ->
            if (baseInfo.status == SdkStatus.UNSUPPORTED) {
                baseInfo
            } else {
                val jar = findAndroidJarForApi(baseInfo.apiLevel)
                if (jar != null && jar.exists() && jar.length() > 0) {
                    baseInfo.copy(
                        androidJarPath = jar.absolutePath,
                        status = SdkStatus.READY
                    )
                } else {
                    baseInfo.copy(
                        androidJarPath = null,
                        status = SdkStatus.MISSING
                    )
                }
            }
        }
    }

    fun findAndroidJarForApi(apiLevel: Int): File? {
        // 1. Check direct file candidates
        for (sdkDir in candidateSdkDirs) {
            // Check <sdkDir>/android-<api>/android.jar
            val platformFolderJar = File(sdkDir, "android-$apiLevel/android.jar")
            if (platformFolderJar.exists() && platformFolderJar.canRead() && platformFolderJar.length() > 0) {
                return platformFolderJar
            }
            // Check <sdkDir>/platforms/android-<api>/android.jar
            val subPlatformJar = File(sdkDir, "platforms/android-$apiLevel/android.jar")
            if (subPlatformJar.exists() && subPlatformJar.canRead() && subPlatformJar.length() > 0) {
                return subPlatformJar
            }
            // Check direct android-<api>.jar
            val directJar = File(sdkDir, "android-$apiLevel.jar")
            if (directJar.exists() && directJar.canRead() && directJar.length() > 0) {
                return directJar
            }
        }

        // 2. Try extracting from app assets if present inside packaged APK
        val extractedJar = extractJarFromAssets(apiLevel)
        if (extractedJar != null && extractedJar.exists() && extractedJar.length() > 0) {
            return extractedJar
        }

        // 3. Fallback to app/libs/android.jar
        val libsJar = File("/storage/emulated/0/test-folder/ide-studio/app/libs/android.jar")
        if (libsJar.exists() && libsJar.canRead() && libsJar.length() > 0) {
            return libsJar
        }

        // 4. System framework fallback: /system/framework/framework.jar
        val frameworkJar = File("/system/framework/framework.jar")
        if (frameworkJar.exists() && frameworkJar.canRead()) {
            return frameworkJar
        }

        return null
    }

    private fun extractJarFromAssets(apiLevel: Int): File? {
        val destDir = File(context.filesDir, "platform/android-$apiLevel")
        val destJar = File(destDir, "android.jar")
        if (destJar.exists() && destJar.length() > 0) {
            return destJar
        }

        val assetPaths = listOf(
            "platform/android-$apiLevel/android.jar",
            "platforms/android-$apiLevel/android.jar",
            "android.jar"
        )

        for (assetPath in assetPaths) {
            try {
                context.assets.open(assetPath).use { input ->
                    destDir.mkdirs()
                    val tempFile = File(destDir, "android.jar.tmp")
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                    tempFile.renameTo(destJar)
                    if (destJar.exists() && destJar.length() > 0) {
                        return destJar
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun getPrimarySdkDir(): File {
        return candidateSdkDirs.firstOrNull { it.exists() } ?: candidateSdkDirs[0]
    }
}
