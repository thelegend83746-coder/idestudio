package com.idestudio.app.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipManager {

    fun zipDirectory(sourceDir: File, outputZip: File): Boolean {
        return try {
            outputZip.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(outputZip)).use { zos ->
                val baseDirPath = sourceDir.canonicalPath
                sourceDir.walkTopDown().forEach { file ->
                    val relativePath = file.canonicalPath.substring(baseDirPath.length).let {
                        if (it.startsWith(File.separator)) it.substring(1) else it
                    }
                    if (relativePath.isNotEmpty()) {
                        if (file.isDirectory) {
                            val entry = ZipEntry("$relativePath/")
                            zos.putNextEntry(entry)
                            zos.closeEntry()
                        } else {
                            val entry = ZipEntry(relativePath)
                            zos.putNextEntry(entry)
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun unzipDirectory(zipFile: File, targetDir: File): Boolean {
        return try {
            targetDir.mkdirs()
            val targetCanonicalPath = targetDir.canonicalPath
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val newFile = File(targetDir, entry.name)
                    // Security check: Path traversal prevention
                    val newFileCanonical = newFile.canonicalPath
                    if (!newFileCanonical.startsWith(targetCanonicalPath)) {
                        throw SecurityException("Zip entry is outside of the target dir: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
