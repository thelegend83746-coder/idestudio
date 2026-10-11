package com.idestudio.engine

import android.content.Context
import com.idestudio.data.model.BuildLogEntry
import com.idestudio.data.model.BuildState
import com.idestudio.data.model.BuildStatus
import com.idestudio.data.model.Diagnostic
import com.idestudio.data.model.LogLevel
import com.idestudio.data.model.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BuildEngine(private val context: Context) {

    private val sdkManager = SdkPlatformManager(context)
    private val javaCompiler = JavaCompilerBridge()
    private val resourceCompiler = ResourceCompilerBridge(context)
    private val dexConverter = DexConverterBridge()
    private val packager = ApkPackager()
    private val signer = ApkSigner()

    private val _buildState = MutableStateFlow(BuildState())
    val buildState: StateFlow<BuildState> = _buildState.asStateFlow()

    private var activeBuildJob: Job? = null

    fun cancelBuild() {
        activeBuildJob?.cancel()
        _buildState.update { current ->
            current.copy(
                status = BuildStatus.CANCELLED,
                stageMessage = "Build cancelled by user",
                endTimeMs = System.currentTimeMillis(),
                logs = current.logs + BuildLogEntry(
                    level = LogLevel.WARNING,
                    stage = BuildStatus.CANCELLED,
                    message = "Build execution cancelled by user."
                )
            )
        }
    }

    fun startBuild(project: Project, scope: CoroutineScope) {
        if (_buildState.value.isRunning) return

        activeBuildJob = scope.launch(Dispatchers.Default) {
            executeBuildPipeline(project)
        }
    }

    private suspend fun executeBuildPipeline(project: Project) = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        _buildState.value = BuildState(
            status = BuildStatus.PREPARING,
            stageMessage = "Initializing build environment...",
            startTimeMs = startTime,
            logs = listOf(
                BuildLogEntry(
                    level = LogLevel.INFO,
                    stage = BuildStatus.PREPARING,
                    message = "Starting on-device build for project: ${project.name} (${project.packageName})"
                )
            )
        )

        try {
            // Stage 1: Preparation & Directory Setup
            val buildDir = File(project.buildDir).apply { mkdirs() }
            val genDir = File(buildDir, "generated/source/r").apply { mkdirs() }
            val classesDir = File(buildDir, "intermediates/classes").apply { mkdirs() }
            val dexDir = File(buildDir, "intermediates/dex").apply { mkdirs() }
            val outputsDir = File(buildDir, "outputs").apply { mkdirs() }

            log(BuildStatus.PREPARING, LogLevel.INFO, "Build directories configured at ${buildDir.absolutePath}")

            // Stage 2: SDK Discovery
            log(BuildStatus.PREPARING, LogLevel.INFO, "Discovering Android platform API ${project.targetSdk}...")
            val androidJar = sdkManager.findAndroidJarForApi(project.targetSdk)
            val classpathJars = mutableListOf<File>()
            if (androidJar != null && androidJar.exists()) {
                log(BuildStatus.PREPARING, LogLevel.INFO, "Android platform jar found: ${androidJar.absolutePath}")
                classpathJars.add(androidJar)
            } else {
                log(
                    BuildStatus.PREPARING,
                    LogLevel.WARNING,
                    "android.jar for API ${project.targetSdk} not found in local SDK path. Build will attempt compilation with available runtime classpaths."
                )
            }

            // Stage 3: Generate R.java
            updateStage(BuildStatus.PROCESSING_RESOURCES, "Generating R.java from project resources...")
            val generatedRFile = RJavaGenerator.generateRJava(project, genDir)
            log(BuildStatus.PROCESSING_RESOURCES, LogLevel.INFO, "Generated R.java symbol table at ${generatedRFile.absolutePath}")

            // Stage 4: Process XML Resources & Manifest
            updateStage(BuildStatus.PROCESSING_RESOURCES, "Validating XML layouts, values, and manifest...")
            val resResult = resourceCompiler.processResources(project, androidJar, File(buildDir, "intermediates/res"))
            if (!resResult.success) {
                log(BuildStatus.PROCESSING_RESOURCES, LogLevel.ERROR, resResult.outputLog)
                failBuild("Resource processing failed", resResult.diagnostics)
                return@withContext
            }
            log(BuildStatus.PROCESSING_RESOURCES, LogLevel.INFO, resResult.outputLog.trim())

            // Stage 5: Compile Java
            updateStage(BuildStatus.COMPILING_JAVA, "Compiling Java sources with ECJ...")
            val srcDir = File(project.rootPath, "app/src/main/java")
            val sourceFiles = mutableListOf<File>()
            if (srcDir.exists()) {
                sourceFiles.addAll(srcDir.walkTopDown().filter { it.isFile && it.extension == "java" })
            }
            sourceFiles.add(generatedRFile)

            log(BuildStatus.COMPILING_JAVA, LogLevel.INFO, "Compiling ${sourceFiles.size} Java source files...")
            val compResult = javaCompiler.compile(sourceFiles, classpathJars, classesDir)

            if (!compResult.success) {
                log(BuildStatus.COMPILING_JAVA, LogLevel.ERROR, compResult.outputLog)
                failBuild("Java compilation failed with ${compResult.diagnostics.size} errors", compResult.diagnostics)
                return@withContext
            }
            log(BuildStatus.COMPILING_JAVA, LogLevel.INFO, "Java compilation succeeded: ${compResult.classFiles.size} classes generated.")

            // Stage 6: Bytecode conversion to DEX
            updateStage(BuildStatus.CONVERTING_BYTECODE, "Converting Java bytecode to classes.dex with D8...")
            val dexResult = dexConverter.convertClassesToDex(classesDir, androidJar, dexDir, project.minSdk)
            if (!dexResult.success) {
                log(BuildStatus.CONVERTING_BYTECODE, LogLevel.ERROR, dexResult.outputLog)
                failBuild("DEX conversion failed: ${dexResult.outputLog}", dexResult.diagnostics)
                return@withContext
            }
            log(BuildStatus.CONVERTING_BYTECODE, LogLevel.INFO, "Bytecode conversion succeeded: classes.dex generated.")

            // Stage 7: Assemble APK
            updateStage(BuildStatus.PACKAGING, "Packaging APK archive...")
            val unalignedApk = File(buildDir, "intermediates/app-unaligned.apk")
            val packageSuccess = packager.packageApk(project, dexResult.dexFiles, unalignedApk)
            if (!packageSuccess) {
                failBuild("Failed to package unaligned APK", emptyList())
                return@withContext
            }
            log(BuildStatus.PACKAGING, LogLevel.INFO, "Packaged archive created: ${unalignedApk.length()} bytes.")

            // Stage 8: Align APK
            updateStage(BuildStatus.PACKAGING, "Aligning APK (4-byte alignment)...")
            val alignedApk = File(buildDir, "intermediates/app-aligned.apk")
            ZipAligner.align(unalignedApk, alignedApk)

            // Stage 9: Sign APK
            updateStage(BuildStatus.SIGNING, "Signing APK with debug keystore...")
            val finalApk = File(project.outputApkPath)
            finalApk.parentFile?.mkdirs()
            val keystoreFile = File(context.filesDir, "security/debug.keystore")
            val signed = signer.signApk(alignedApk, finalApk, keystoreFile)
            if (!signed) {
                failBuild("Failed to sign APK archive", emptyList())
                return@withContext
            }
            log(BuildStatus.SIGNING, LogLevel.INFO, "APK cryptographically signed.")

            // Stage 10: Verify APK
            updateStage(BuildStatus.VERIFYING, "Verifying signed APK integrity and structure...")
            val verifyResult = ApkVerifier.verify(finalApk)
            if (!verifyResult.isValid) {
                failBuild("APK verification failed: ${verifyResult.failureReason}", emptyList())
                return@withContext
            }
            log(BuildStatus.VERIFYING, LogLevel.INFO, "Verification passed: SHA-256 = ${verifyResult.sha256Checksum.take(16)}...")

            // Completed Successfully
            val endTime = System.currentTimeMillis()
            _buildState.update { current ->
                current.copy(
                    status = BuildStatus.SUCCESS,
                    stageMessage = "Build completed successfully!",
                    endTimeMs = endTime,
                    outputApkPath = finalApk.absolutePath,
                    apkSizeBytes = finalApk.length(),
                    logs = current.logs + BuildLogEntry(
                        level = LogLevel.INFO,
                        stage = BuildStatus.SUCCESS,
                        message = "Build succeeded in ${(endTime - startTime) / 1000.0}s. Output: ${finalApk.absolutePath} (${finalApk.length()} bytes)"
                    )
                )
            }

        } catch (e: CancellationException) {
            cancelBuild()
        } catch (e: Exception) {
            failBuild("Unexpected build error: ${e.message}", emptyList())
        }
    }

    private fun updateStage(status: BuildStatus, message: String) {
        _buildState.update { it.copy(status = status, stageMessage = message) }
    }

    private fun log(stage: BuildStatus, level: LogLevel, message: String, diagnostic: Diagnostic? = null) {
        _buildState.update { current ->
            current.copy(
                logs = current.logs + BuildLogEntry(
                    level = level,
                    stage = stage,
                    message = message,
                    diagnostic = diagnostic
                )
            )
        }
    }

    private fun failBuild(errorMessage: String, diagnostics: List<Diagnostic>) {
        val endTime = System.currentTimeMillis()
        _buildState.update { current ->
            current.copy(
                status = BuildStatus.FAILURE,
                stageMessage = errorMessage,
                endTimeMs = endTime,
                errorDiagnostics = diagnostics,
                logs = current.logs + BuildLogEntry(
                    level = LogLevel.ERROR,
                    stage = BuildStatus.FAILURE,
                    message = "BUILD FAILED: $errorMessage"
                )
            )
        }
    }
}
