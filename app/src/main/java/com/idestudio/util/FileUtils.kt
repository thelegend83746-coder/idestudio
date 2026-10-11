package com.idestudio.util

import java.io.File
import java.io.IOException

object FileUtils {

    @Throws(IOException::class)
    fun readFileSafe(file: File): String {
        if (!file.exists() || !file.canRead()) {
            throw IOException("File cannot be read: ${file.absolutePath}")
        }
        return file.readText(Charsets.UTF_8)
    }

    @Throws(IOException::class)
    fun writeFileSafe(file: File, content: String) {
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs() && !parent.exists()) {
                throw IOException("Failed to create parent directories for: ${file.absolutePath}")
            }
        }
        val tempFile = File(parent, "${file.name}.tmp")
        tempFile.writeText(content, Charsets.UTF_8)
        if (file.exists()) {
            if (!file.delete()) {
                throw IOException("Failed to delete existing file: ${file.absolutePath}")
            }
        }
        if (!tempFile.renameTo(file)) {
            // Fallback direct write
            file.writeText(content, Charsets.UTF_8)
            tempFile.delete()
        }
    }

    fun createBackup(file: File): File? {
        if (!file.exists()) return null
        val backupFile = File(file.parentFile, "${file.name}.bak")
        return try {
            file.copyTo(backupFile, overwrite = true)
            backupFile
        } catch (_: Exception) {
            null
        }
    }

    fun restoreBackup(backupFile: File, targetFile: File): Boolean {
        if (!backupFile.exists()) return false
        return try {
            backupFile.copyTo(targetFile, overwrite = true)
            backupFile.delete()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun deleteRecursivelySafe(fileOrDir: File): Boolean {
        if (!fileOrDir.exists()) return true
        if (fileOrDir.isDirectory) {
            val children = fileOrDir.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteRecursivelySafe(child)
                }
            }
        }
        return fileOrDir.delete()
    }
}
