package com.idestudio.engine

import com.idestudio.data.model.Diagnostic
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.Method
import java.util.regex.Pattern

data class CompilationResult(
    val success: Boolean,
    val outputLog: String,
    val diagnostics: List<Diagnostic>,
    val classFiles: List<File>
)

class JavaCompilerBridge {

    private val ecjErrorPattern1 = Pattern.compile("(?m)^(\\d+)\\. ERROR in (.+?) \\(at line (\\d+)\\)\\s*\\n\\s*(?:.*?)\\s*\\n\\s*(?:.*?)\\s*\\n\\s*(.+)$")
    private val standardErrorPattern = Pattern.compile("(?m)^(.+?\\.java):(\\d+):\\s*(?:error:\\s*)?(.+)$")

    fun compile(
        sourceFiles: List<File>,
        classpathJars: List<File>,
        outputClassesDir: File
    ): CompilationResult {
        if (!outputClassesDir.exists()) {
            outputClassesDir.mkdirs()
        }

        val outWriter = StringWriter()
        val errWriter = StringWriter()
        val outPrinter = PrintWriter(outWriter)
        val errPrinter = PrintWriter(errWriter)

        val cpString = classpathJars
            .filter { it.exists() }
            .joinToString(File.pathSeparator) { it.absolutePath }

        val argsList = mutableListOf<String>()
        argsList.add("-1.8") // Java 8 bytecode compatibility for D8
        argsList.add("-encoding")
        argsList.add("UTF-8")
        argsList.add("-d")
        argsList.add(outputClassesDir.absolutePath)
        if (cpString.isNotBlank()) {
            argsList.add("-classpath")
            argsList.add(cpString)
        }
        for (f in sourceFiles) {
            argsList.add(f.absolutePath)
        }

        var success = false
        try {
            // Attempt 1: Invoke Eclipse Compiler for Java (ECJ) via reflection
            val ecjClass = try {
                Class.forName("org.eclipse.jdt.internal.compiler.batch.Main")
            } catch (_: ClassNotFoundException) {
                null
            }

            if (ecjClass != null) {
                val constructor = ecjClass.getConstructor(
                    PrintWriter::class.java,
                    PrintWriter::class.java,
                    Boolean::class.javaPrimitiveType
                )
                val mainInstance = constructor.newInstance(outPrinter, errPrinter, false)
                val compileMethod: Method = ecjClass.getMethod("compile", Array<String>::class.java)
                val ret = compileMethod.invoke(mainInstance, argsList.toTypedArray()) as? Boolean ?: false
                success = ret
            } else {
                // Attempt 2: Fallback to System Java Compiler (javax.tools) if available
                val sysCompilerClass = try {
                    Class.forName("javax.tools.ToolProvider")
                } catch (_: Exception) {
                    null
                }
                if (sysCompilerClass != null) {
                    val getCompilerMethod = sysCompilerClass.getMethod("getSystemJavaCompiler")
                    val compiler = getCompilerMethod.invoke(null)
                    if (compiler != null) {
                        val runMethod = compiler.javaClass.getMethod(
                            "run",
                            java.io.InputStream::class.java,
                            java.io.OutputStream::class.java,
                            java.io.OutputStream::class.java,
                            Array<String>::class.java
                        )
                        val exitCode = runMethod.invoke(compiler, null, null, null, argsList.toTypedArray()) as Int
                        success = (exitCode == 0)
                    } else {
                        errPrinter.println("No Java compiler (ECJ or javax.tools) found on device runtime.")
                        success = false
                    }
                } else {
                    errPrinter.println("ECJ compiler library not found in classpath. Please verify build configuration.")
                    success = false
                }
            }
        } catch (e: Exception) {
            errPrinter.println("Java compilation invocation error: ${e.message}")
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

        val diagnostics = parseDiagnostics(fullLog)
        val compiledClasses = outputClassesDir.walkTopDown().filter { it.isFile && it.extension == "class" }.toList()

        return CompilationResult(
            success = success && compiledClasses.isNotEmpty(),
            outputLog = fullLog,
            diagnostics = diagnostics,
            classFiles = compiledClasses
        )
    }

    private fun parseDiagnostics(rawOutput: String): List<Diagnostic> {
        val list = mutableListOf<Diagnostic>()

        // Check standard format (e.g. MainActivity.java:15: error: ...)
        val stdMatcher = standardErrorPattern.matcher(rawOutput)
        while (stdMatcher.find()) {
            val file = stdMatcher.group(1)
            val line = stdMatcher.group(2)?.toIntOrNull()
            val msg = stdMatcher.group(3) ?: "Compiler error"
            list.add(Diagnostic(filePath = file, lineNumber = line, message = msg, isError = true))
        }

        // Check ECJ format
        val ecjMatcher = ecjErrorPattern1.matcher(rawOutput)
        while (ecjMatcher.find()) {
            val file = ecjMatcher.group(2)
            val line = ecjMatcher.group(3)?.toIntOrNull()
            val msg = ecjMatcher.group(4) ?: "Compiler error"
            list.add(Diagnostic(filePath = file, lineNumber = line, message = msg, isError = true))
        }

        return list
    }
}
