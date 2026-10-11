package com.idestudio.engine

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

data class ApkVerificationResult(
    val isValid: Boolean,
    val hasClassesDex: Boolean,
    val hasManifest: Boolean,
    val isSigned: Boolean,
    val sizeBytes: Long,
    val sha256Checksum: String,
    val failureReason: String? = null
)

object ApkVerifier {

    fun verify(apkFile: File): ApkVerificationResult {
        if (!apkFile.exists()) {
            return ApkVerificationResult(
                isValid = false,
                hasClassesDex = false,
                hasManifest = false,
                isSigned = false,
                sizeBytes = 0L,
                sha256Checksum = "",
                failureReason = "APK file does not exist on disk: ${apkFile.absolutePath}"
            )
        }

        if (apkFile.length() == 0L) {
            return ApkVerificationResult(
                isValid = false,
                hasClassesDex = false,
                hasManifest = false,
                isSigned = false,
                sizeBytes = 0L,
                sha256Checksum = "",
                failureReason = "APK file is empty (0 bytes)"
            )
        }

        var foundDex = false
        var foundManifest = false
        var foundCertRsa = false
        var foundCertSf = false

        try {
            ZipInputStream(FileInputStream(apkFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        "classes.dex" -> foundDex = true
                        "AndroidManifest.xml" -> foundManifest = true
                        "META-INF/CERT.RSA", "META-INF/CERT.DSA" -> foundCertRsa = true
                        "META-INF/CERT.SF" -> foundCertSf = true
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            return ApkVerificationResult(
                isValid = false,
                hasClassesDex = false,
                hasManifest = false,
                isSigned = false,
                sizeBytes = apkFile.length(),
                sha256Checksum = "",
                failureReason = "Failed to parse APK archive: ${e.message}"
            )
        }

        val isSigned = foundCertRsa && foundCertSf
        val isValid = foundDex && foundManifest && isSigned

        val md = MessageDigest.getInstance("SHA-256")
        val sha256 = apkFile.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } > 0) {
                md.update(buffer, 0, read)
            }
            md.digest().joinToString("") { "%02x".format(it) }
        }

        val failure = when {
            !foundDex -> "Missing classes.dex bytecode inside APK"
            !foundManifest -> "Missing AndroidManifest.xml inside APK"
            !isSigned -> "Missing valid META-INF signature files"
            else -> null
        }

        return ApkVerificationResult(
            isValid = isValid,
            hasClassesDex = foundDex,
            hasManifest = foundManifest,
            isSigned = isSigned,
            sizeBytes = apkFile.length(),
            sha256Checksum = sha256,
            failureReason = failure
        )
    }
}
