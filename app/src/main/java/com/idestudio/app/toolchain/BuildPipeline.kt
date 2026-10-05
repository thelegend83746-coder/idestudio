package com.idestudio.app.toolchain

import com.idestudio.app.data.model.BuildLogEntry
import com.idestudio.app.data.model.BuildStatus
import com.idestudio.app.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class BuildPipeline(
    private val toolchainManager: ToolchainManager
) {

    private val _buildStatus = MutableStateFlow(BuildStatus.IDLE)
    val buildStatus: StateFlow<BuildStatus> = _buildStatus.asStateFlow()

    private val _logs = MutableStateFlow<List<BuildLogEntry>>(emptyList())
    val logs: StateFlow<List<BuildLogEntry>> = _logs.asStateFlow()

    private val _generatedApk = MutableStateFlow<File?>(null)
    val generatedApk: StateFlow<File?> = _generatedApk.asStateFlow()

    private val _buildDurationMs = MutableStateFlow(0L)
    val buildDurationMs: StateFlow<Long> = _buildDurationMs.asStateFlow()

    fun reset() {
        _buildStatus.value = BuildStatus.IDLE
        _logs.value = emptyList()
        _generatedApk.value = null
        _buildDurationMs.value = 0L
    }

    private fun log(stage: String, message: String, isError: Boolean = false, isWarning: Boolean = false, isHeader: Boolean = false) {
        val entry = BuildLogEntry(
            stage = stage,
            message = message,
            isError = isError,
            isWarning = isWarning,
            isHeader = isHeader
        )
        _logs.value = _logs.value + entry
    }

    private fun logEntry(entry: BuildLogEntry) {
        _logs.value = _logs.value + entry
    }

    suspend fun executeBuild(project: Project): Boolean = withContext(Dispatchers.IO) {
        if (_buildStatus.value == BuildStatus.RUNNING) {
            return@withContext false
        }

        val startTime = System.currentTimeMillis()
        _buildStatus.value = BuildStatus.RUNNING
        _logs.value = emptyList()
        _generatedApk.value = null

        log("Validation", "Starting build for project: ${project.name} (${project.packageName})", isHeader = true)

        try {
            // Stage 1: Validate Project Structure
            log("Validation", "Checking project directory structure...")
            if (!project.rootDir.exists()) {
                log("Validation", "Project directory does not exist at ${project.rootDir.absolutePath}", isError = true)
                failBuild(startTime)
                return@withContext false
            }

            // Stage 2: Validate Manifest
            log("Validation", "Validating AndroidManifest.xml...")
            if (!project.manifestFile.exists()) {
                log("Validation", "AndroidManifest.xml not found at ${project.manifestFile.absolutePath}", isError = true)
                failBuild(startTime)
                return@withContext false
            }
            val manifestText = project.manifestFile.readText()
            if (!manifestText.contains("<manifest") || !manifestText.contains("package=\"${project.packageName}\"")) {
                log("Validation", "Warning: Manifest package tag does not match configured package ${project.packageName}", isWarning = true)
            }

            // Stage 3: Validate SDK / Platform
            log("Validation", "Validating compile SDK (${project.compileSdk}) and min SDK (${project.minSdk})...")
            if (project.minSdk > project.targetSdk) {
                log("Validation", "minSdk (${project.minSdk}) cannot be greater than targetSdk (${project.targetSdk})", isError = true)
                failBuild(startTime)
                return@withContext false
            }

            // Stage 4: Validate Resources
            log("Validation", "Checking resource directory: ${project.resDir.name}...")
            if (!project.resDir.exists()) {
                log("Validation", "res/ directory missing, creating empty res folder...", isWarning = true)
                project.resDir.mkdirs()
            }

            // Prepare build directories
            val buildDir = project.buildDir
            val binDir = File(buildDir, "bin")
            binDir.mkdirs()

            // Stage 5: Compile Resources with AAPT2 / ResourceGenerator
            log("AAPT2", "Compiling resources and generating R.java...", isHeader = true)
            val aapt2Compiler = Aapt2Compiler(
                aapt2Binary = if (toolchainManager.aapt2File.exists()) toolchainManager.aapt2File else null,
                androidJar = if (toolchainManager.androidJarFile.exists()) toolchainManager.androidJarFile else null
            )
            val aapt2Result = aapt2Compiler.compileAndLink(project) { msg, isErr ->
                log("AAPT2", msg, isError = isErr)
            }
            if (!aapt2Result.success) {
                log("AAPT2", "Resource compilation failed.", isError = true)
                failBuild(startTime)
                return@withContext false
            }

            // Stage 6: Compile Java Sources
            log("Java", "Compiling Java source files...", isHeader = true)
            val javaCompiler = JavaCompiler(
                androidJar = if (toolchainManager.androidJarFile.exists()) toolchainManager.androidJarFile else null
            )
            val compilerResult = javaCompiler.compile(project) { entry ->
                logEntry(entry)
            }
            if (!compilerResult.success) {
                log("Java", "Compilation failed with ${compilerResult.totalErrors} error(s).", isError = true)
                failBuild(startTime)
                return@withContext false
            }

            // Stage 7: Generate DEX with D8
            log("D8", "Converting compiled classes to DEX...", isHeader = true)
            val dexGenerator = DexGenerator(
                customD8Bin = if (toolchainManager.d8File.exists()) toolchainManager.d8File else null
            )
            val dexResult = dexGenerator.generateDex(project, compilerResult.outputClassesDir) { entry ->
                logEntry(entry)
            }
            if (!dexResult.success || dexResult.outputDexFile == null) {
                log("D8", "DEX generation failed: ${dexResult.errorMessage ?: "Unknown error"}", isError = true)
                failBuild(startTime)
                return@withContext false
            }

            // Stage 8: Package APK
            log("APK", "Packaging APK contents...", isHeader = true)
            val apkPackager = ApkPackager()
            val packageResult = apkPackager.packageApk(
                project = project,
                dexFile = dexResult.outputDexFile,
                baseApk = aapt2Result.compiledApkOrZip
            ) { entry ->
                logEntry(entry)
            }
            if (!packageResult.success || packageResult.outputApk == null) {
                log("APK", "Packaging failed.", isError = true)
                failBuild(startTime)
                return@withContext false
            }

            // Stage 9: Align APK (zipalign)
            log("zipalign", "Aligning APK...", isHeader = true)
            val zipAligner = ZipAligner(
                customZipAlignBin = if (toolchainManager.zipAlignFile.exists()) toolchainManager.zipAlignFile else null
            )
            val alignedApk = File(binDir, "aligned.apk")
            val alignResult = zipAligner.align(packageResult.outputApk, alignedApk) { entry ->
                logEntry(entry)
            }
            if (!alignResult.success || alignResult.outputAlignedApk == null) {
                log("zipalign", "APK alignment failed.", isError = true)
                failBuild(startTime)
                return@withContext false
            }

            // Stage 10: Sign APK (apksigner)
            log("apksigner", "Signing APK...", isHeader = true)
            val finalOutputApk = project.outputApk
            val apkSigner = ApkSigner(
                customApkSignerBin = if (toolchainManager.apkSignerFile.exists()) toolchainManager.apkSignerFile else null
            )
            val signResult = apkSigner.signApk(alignResult.outputAlignedApk, finalOutputApk) { entry ->
                logEntry(entry)
            }
            if (!signResult.success || signResult.signedApk == null) {
                log("apksigner", "APK signing failed.", isError = true)
                failBuild(startTime)
                return@withContext false
            }

            // Stage 11: Verify APK Signature
            log("Verify", "Verifying signed APK...", isHeader = true)
            val verified = apkSigner.verifyApk(finalOutputApk) { entry ->
                logEntry(entry)
            }
            if (!verified) {
                log("Verify", "APK verification warning: signature could not be verified.", isWarning = true)
            }

            // Stage 12: Output & Finish
            val elapsed = System.currentTimeMillis() - startTime
            _buildDurationMs.value = elapsed
            _generatedApk.value = finalOutputApk
            _buildStatus.value = BuildStatus.SUCCESS

            log("Done", "BUILD SUCCESSFUL in ${String.format("%.2f", elapsed / 1000.0)}s", isHeader = true)
            log("Done", "Output: ${finalOutputApk.absolutePath} (${finalOutputApk.length()} bytes)")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            log("Error", "Build aborted due to unhandled exception: ${e.message}", isError = true)
            failBuild(startTime)
            false
        }
    }

    private fun failBuild(startTime: Long) {
        val elapsed = System.currentTimeMillis() - startTime
        _buildDurationMs.value = elapsed
        _buildStatus.value = BuildStatus.FAILED
        log("Done", "BUILD FAILED in ${String.format("%.2f", elapsed / 1000.0)}s", isError = true, isHeader = true)
    }
}
