package com.idestudio.app.toolchain

import com.idestudio.app.data.model.BuildLogEntry
import com.idestudio.app.data.model.Project
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkPackager {

    data class PackageResult(
        val success: Boolean,
        val outputApk: File?,
        val errorMessage: String? = null
    )

    fun packageApk(
        project: Project,
        dexFile: File,
        baseApk: File?,
        logCallback: (BuildLogEntry) -> Unit
    ): PackageResult {
        val binDir = File(project.buildDir, "bin")
        binDir.mkdirs()
        val unalignedApk = File(binDir, "unaligned.apk")

        logCallback(BuildLogEntry("APK", "Packaging classes.dex, manifest, and resources into unaligned APK..."))

        return try {
            ZipOutputStream(FileOutputStream(unalignedApk)).use { zos ->
                // 1. Add AndroidManifest.xml
                if (project.manifestFile.exists()) {
                    addFileToZip(zos, project.manifestFile, "AndroidManifest.xml")
                }

                // 2. Add classes.dex
                if (dexFile.exists()) {
                    addFileToZip(zos, dexFile, "classes.dex")
                }

                // 3. Add resources from project res/
                if (project.resDir.exists()) {
                    project.resDir.walkTopDown().filter { it.isFile }.forEach { resFile ->
                        val relative = resFile.relativeTo(project.resDir).path.replace('\\', '/')
                        addFileToZip(zos, resFile, "res/$relative")
                    }
                }

                // 4. Add assets if present
                val assetsDir = File(project.srcDir, "assets")
                if (assetsDir.exists()) {
                    assetsDir.walkTopDown().filter { it.isFile }.forEach { assetFile ->
                        val relative = assetFile.relativeTo(assetsDir).path.replace('\\', '/')
                        addFileToZip(zos, assetFile, "assets/$relative")
                    }
                }
            }

            logCallback(BuildLogEntry("APK", "Created unaligned APK at ${unalignedApk.name} (${unalignedApk.length()} bytes)"))
            PackageResult(true, unalignedApk)
        } catch (e: Exception) {
            val err = "APK packaging failed: ${e.message}"
            logCallback(BuildLogEntry("APK", err, isError = true))
            PackageResult(false, null, err)
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, entryPath: String) {
        val entry = ZipEntry(entryPath)
        entry.time = file.lastModified()
        zos.putNextEntry(entry)
        FileInputStream(file).use { fis ->
            fis.copyTo(zos)
        }
        zos.closeEntry()
    }
}
