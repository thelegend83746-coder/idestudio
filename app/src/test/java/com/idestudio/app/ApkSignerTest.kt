package com.idestudio.app

import com.idestudio.app.toolchain.ApkSigner
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkSignerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSignAndVerifyApk() {
        val unsignedApk = tempFolder.newFile("unsigned.apk")
        ZipOutputStream(FileOutputStream(unsignedApk)).use { zos ->
            zos.putNextEntry(ZipEntry("classes.dex"))
            zos.write("dummy dex content".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zos.write("<manifest/>".toByteArray())
            zos.closeEntry()
        }

        val signedApk = tempFolder.newFile("signed.apk")
        val signer = ApkSigner()
        val result = signer.signApk(unsignedApk, signedApk) {}

        assertTrue("APK signing should succeed", result.success)
        assertTrue("Signed APK file should exist", signedApk.exists())
        assertTrue("Signed APK should not be empty", signedApk.length() > 0)

        val verified = signer.verifyApk(signedApk) {}
        assertTrue("Signature verification should succeed", verified)
    }
}
