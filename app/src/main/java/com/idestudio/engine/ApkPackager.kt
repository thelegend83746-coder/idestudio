package com.idestudio.engine

import com.idestudio.data.model.Project
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkPackager {

    fun packageApk(
        project: Project,
        dexFiles: List<File>,
        outputUnsignedApk: File
    ): Boolean {
        val parent = outputUnsignedApk.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()

        if (outputUnsignedApk.exists()) outputUnsignedApk.delete()

        ZipOutputStream(FileOutputStream(outputUnsignedApk)).use { zos ->
            // 1. Add classes.dex
            for (dexFile in dexFiles) {
                if (dexFile.exists()) {
                    addFileToZip(zos, dexFile, dexFile.name, compress = true)
                }
            }

            // 2. Add AndroidManifest.xml
            val manifestFile = File(project.manifestPath)
            if (manifestFile.exists()) {
                addFileToZip(zos, manifestFile, "AndroidManifest.xml", compress = true)
            }

            // 3. Add resources from res/
            val resDir = File(project.resDir)
            if (resDir.exists()) {
                resDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val relativePath = "res/" + file.relativeTo(resDir).path.replace('\\', '/')
                    addFileToZip(zos, file, relativePath, compress = true)
                }
            }

            // 4. Add assets/
            val assetsDir = File(project.rootPath, "app/src/main/assets")
            if (assetsDir.exists()) {
                assetsDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val relativePath = "assets/" + file.relativeTo(assetsDir).path.replace('\\', '/')
                    addFileToZip(zos, file, relativePath, compress = false)
                }
            }
        }

        return outputUnsignedApk.exists() && outputUnsignedApk.length() > 0
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, zipPath: String, compress: Boolean) {
        val bytes = file.readBytes()
        val entry = ZipEntry(zipPath)
        if (!compress) {
            entry.method = ZipEntry.STORED
            entry.size = bytes.size.toLong()
            entry.compressedSize = bytes.size.toLong()
            val crc = CRC32()
            crc.update(bytes)
            entry.crc = crc.value
        } else {
            entry.method = ZipEntry.DEFLATED
        }
        zos.putNextEntry(entry)
        zos.write(bytes)
        zos.closeEntry()
    }
}
