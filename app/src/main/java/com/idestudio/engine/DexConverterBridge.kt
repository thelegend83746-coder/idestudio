package com.idestudio.engine

import com.idestudio.data.model.Diagnostic
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.Method

data class DexResult(
    val success: Boolean,
    val outputLog: String,
    val dexFiles: List<File>,
    val diagnostics: List<Diagnostic>
)

class DexConverterBridge {

    fun convertClassesToDex(
        classesDir: File,
        androidJar: File?,
        outputDexDir: File,
        minApi: Int
    ): DexResult {
        if (!outputDexDir.exists()) {
            outputDexDir.mkdirs()
        }

        val outWriter = StringWriter()
        val errWriter = StringWriter()
        val outPrinter = PrintWriter(outWriter)
        val errPrinter = PrintWriter(errWriter)

        val classFiles = classesDir.walkTopDown().filter { it.isFile && it.extension == "class" }.toList()
        if (classFiles.isEmpty()) {
            return DexResult(
                success = false,
                outputLog = "No compiled .class files found in ${classesDir.absolutePath}",
                dexFiles = emptyList(),
                diagnostics = listOf(Diagnostic(filePath = null, lineNumber = null, message = "No class files to dex"))
            )
        }

        val argsList = mutableListOf<String>()
        argsList.add("--output")
        argsList.add(outputDexDir.absolutePath)
        argsList.add("--min-api")
        argsList.add(minApi.toString())

        if (androidJar != null && androidJar.exists()) {
            argsList.add("--lib")
            argsList.add(androidJar.absolutePath)
        }

        for (cf in classFiles) {
            argsList.add(cf.absolutePath)
        }

        var success = false

        try {
            // Attempt 1: Invoke D8 compiler (com.android.tools.r8.D8)
            val d8Class = try {
                Class.forName("com.android.tools.r8.D8")
            } catch (_: ClassNotFoundException) {
                null
            }

            if (d8Class != null) {
                val mainMethod: Method = d8Class.getMethod("main", Array<String>::class.java)
                mainMethod.invoke(null, argsList.toTypedArray())
                val dexOutput = File(outputDexDir, "classes.dex")
                success = dexOutput.exists() && dexOutput.length() > 0
                outPrinter.println("D8 converted ${classFiles.size} classes into classes.dex")
            } else {
                // Attempt 2: Legacy DX dexer (com.android.dx.command.Main)
                val dxClass = try {
                    Class.forName("com.android.dx.command.Main")
                } catch (_: ClassNotFoundException) {
                    null
                }
                if (dxClass != null) {
                    val dxArgs = arrayOf(
                        "--dex",
                        "--output=${File(outputDexDir, "classes.dex").absolutePath}",
                        classesDir.absolutePath
                    )
                    val mainMethod: Method = dxClass.getMethod("main", Array<String>::class.java)
                    mainMethod.invoke(null, dxArgs)
                    val dexOutput = File(outputDexDir, "classes.dex")
                    success = dexOutput.exists() && dexOutput.length() > 0
                } else {
                    errPrinter.println("D8/DX bytecode conversion tool is not loaded on this runtime classpath.")
                    success = false
                }
            }
        } catch (e: Exception) {
            errPrinter.println("Bytecode DEX conversion error: ${e.message}")
            e.printStackTrace(errPrinter)
            success = false
        }

        outPrinter.flush()
        errPrinter.flush()

        val fullLog = buildString {
            val stdout = outWriter.toString().trim()
            val stderr = errWriter.toString().trim()
            if (stdout.isNotBlank()) append(stdout).append("\n")
            if (stderr.isNotBlank()) append(stderr)
        }

        val dexFiles = outputDexDir.listFiles()?.filter { it.extension == "dex" } ?: emptyList()

        return DexResult(
            success = success,
            outputLog = fullLog,
            dexFiles = dexFiles,
            diagnostics = emptyList()
        )
    }
}
