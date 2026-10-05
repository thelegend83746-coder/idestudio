package com.idestudio.app.toolchain

import com.idestudio.app.data.model.Project
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class Aapt2Compiler(
    private val aapt2Binary: File?,
    private val androidJar: File?
) {

    data class CompileResult(
        val success: Boolean,
        val outputText: String,
        val rJavaFile: File?,
        val compiledApkOrZip: File?
    )

    fun compileAndLink(
        project: Project,
        logCallback: (String, Boolean) -> Unit
    ): CompileResult {
        val genDir = File(project.buildDir, "gen")
        genDir.mkdirs()

        // If AAPT2 binary and android.jar are available and executable, run AAPT2
        if (aapt2Binary != null && aapt2Binary.exists() && aapt2Binary.canExecute() && androidJar != null && androidJar.exists()) {
            logCallback("Executing AAPT2 compile and link pipeline...", false)
            try {
                val compiledResZip = File(project.buildDir, "compiled_res.zip")
                val unalignedApk = File(project.buildDir, "bin/unaligned.apk")
                unalignedApk.parentFile?.mkdirs()

                // AAPT2 compile
                val compileCmd = arrayOf(
                    aapt2Binary.absolutePath,
                    "compile",
                    "--dir", project.resDir.absolutePath,
                    "-o", compiledResZip.absolutePath
                )
                val compileProcess = ProcessBuilder(*compileCmd).redirectErrorStream(true).start()
                val compileLog = compileProcess.inputStream.bufferedReader().use { it.readText() }
                val compileCode = compileProcess.waitFor()

                if (compileCode != 0) {
                    logCallback(compileLog, true)
                    return CompileResult(false, compileLog, null, null)
                }
                logCallback(compileLog.ifEmpty { "AAPT2 resource compile finished." }, false)

                // AAPT2 link
                val linkCmd = arrayOf(
                    aapt2Binary.absolutePath,
                    "link",
                    "-I", androidJar.absolutePath,
                    "--manifest", project.manifestFile.absolutePath,
                    "--java", genDir.absolutePath,
                    "-o", unalignedApk.absolutePath,
                    "--auto-add-overlay",
                    compiledResZip.absolutePath
                )
                val linkProcess = ProcessBuilder(*linkCmd).redirectErrorStream(true).start()
                val linkLog = linkProcess.inputStream.bufferedReader().use { it.readText() }
                val linkCode = linkProcess.waitFor()

                if (linkCode != 0) {
                    logCallback(linkLog, true)
                    return CompileResult(false, linkLog, null, null)
                }
                logCallback(linkLog.ifEmpty { "AAPT2 resource linking finished." }, false)

                val rJava = File(genDir, project.packageName.replace('.', '/') + "/R.java")
                return CompileResult(true, "AAPT2 Success", if (rJava.exists()) rJava else null, unalignedApk)
            } catch (e: Exception) {
                logCallback("AAPT2 execution error: ${e.message}. Falling back to internal Resource Generator...", true)
            }
        }

        // Internal Resource Generator fallback
        logCallback("Running internal Resource Symbol Generator...", false)
        return try {
            val rJava = ResourceGenerator.generateRJava(project, genDir)
            logCallback("Generated ${rJava.name} at ${rJava.parentFile?.name}/${rJava.name}", false)
            CompileResult(true, "Resource generation complete", rJava, null)
        } catch (e: Exception) {
            logCallback("Resource generation failed: ${e.message}", true)
            CompileResult(false, e.message ?: "Failed to generate resources", null, null)
        }
    }
}
