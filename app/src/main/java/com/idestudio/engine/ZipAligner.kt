package com.idestudio.engine

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipAligner {

    fun align(inputApk: File, outputApk: File, alignment: Int = 4): Boolean {
        if (!inputApk.exists()) return false
        if (outputApk.exists()) outputApk.delete()

        try {
            ZipInputStream(FileInputStream(inputApk)).use { zis ->
                ZipOutputStream(FileOutputStream(outputApk)).use { zos ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val newEntry = ZipEntry(entry.name)
                        newEntry.method = entry.method
                        newEntry.time = entry.time
                        if (entry.method == ZipEntry.STORED) {
                            newEntry.size = entry.size
                            newEntry.compressedSize = entry.compressedSize
                            newEntry.crc = entry.crc
                        }
                        zos.putNextEntry(newEntry)
                        zis.copyTo(zos)
                        zos.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            return outputApk.exists() && outputApk.length() > 0
        } catch (_: Exception) {
            // If alignment copy fails, fallback to direct copy
            return try {
                inputApk.copyTo(outputApk, overwrite = true)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
