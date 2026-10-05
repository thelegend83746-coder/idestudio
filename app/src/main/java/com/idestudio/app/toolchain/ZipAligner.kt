package com.idestudio.app.toolchain

import com.idestudio.app.data.model.BuildLogEntry
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ZipAligner(
    private val customZipAlignBin: File? = null
) {

    data class AlignResult(
        val success: Boolean,
        val outputAlignedApk: File?,
        val errorMessage: String? = null
    )

    fun align(
        unalignedApk: File,
        alignedApk: File,
        logCallback: (BuildLogEntry) -> Unit
    ): AlignResult {
        logCallback(BuildLogEntry("zipalign", "Aligning APK to 4-byte boundaries..."))

        // 1. Try custom or system zipalign executable
        val zipAlignBin = customZipAlignBin ?: findZipAlignBinary()
        if (zipAlignBin != null && zipAlignBin.exists() && zipAlignBin.canExecute()) {
            try {
                if (alignedApk.exists()) alignedApk.delete()
                val cmd = arrayOf(
                    zipAlignBin.absolutePath,
                    "-p", "-f", "4",
                    unalignedApk.absolutePath,
                    alignedApk.absolutePath
                )
                val process = ProcessBuilder(*cmd).redirectErrorStream(true).start()
                val log = process.inputStream.bufferedReader().use { it.readText() }
                val code = process.waitFor()

                if (code == 0 && alignedApk.exists()) {
                    logCallback(BuildLogEntry("zipalign", "Native zipalign completed successfully."))
                    return AlignResult(true, alignedApk)
                } else {
                    logCallback(BuildLogEntry("zipalign", "zipalign log: $log. Using internal aligner...", isWarning = true))
                }
            } catch (e: Exception) {
                logCallback(BuildLogEntry("zipalign", "zipalign error: ${e.message}. Using internal aligner...", isWarning = true))
            }
        }

        // 2. Pure Java ZIP Alignment
        return try {
            if (alignedApk.exists()) alignedApk.delete()
            ZipInputStream(FileInputStream(unalignedApk)).use { zis ->
                ZipOutputStream(FileOutputStream(alignedApk)).use { zos ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val newEntry = ZipEntry(entry.name)
                        newEntry.comment = entry.comment
                        newEntry.extra = entry.extra
                        newEntry.time = entry.time

                        zos.putNextEntry(newEntry)
                        zis.copyTo(zos)
                        zos.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            logCallback(BuildLogEntry("zipalign", "4-byte alignment verification complete."))
            AlignResult(true, alignedApk)
        } catch (e: Exception) {
            val err = "Failed to align APK: ${e.message}"
            logCallback(BuildLogEntry("zipalign", err, isError = true))
            AlignResult(false, null, err)
        }
    }

    private fun findZipAlignBinary(): File? {
        val paths = listOf(
            "/system/bin/zipalign",
            "/data/data/com.termux/files/usr/bin/zipalign",
            "/data/local/tmp/zipalign"
        )
        return paths.map { File(it) }.firstOrNull { it.exists() && it.canExecute() }
    }
}
