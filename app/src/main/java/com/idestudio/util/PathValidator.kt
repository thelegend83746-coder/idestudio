package com.idestudio.util

import java.io.File
import java.util.regex.Pattern

object PathValidator {

    private val PACKAGE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)+$")
    private val APP_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\s]{1,50}$")

    fun isValidPackageName(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        val segments = packageName.split('.')
        if (segments.size < 2) return false
        for (segment in segments) {
            if (isJavaKeyword(segment)) return false
        }
        return PACKAGE_NAME_PATTERN.matcher(packageName).matches()
    }

    fun isValidAppName(name: String): Boolean {
        if (name.isBlank() || name.length > 50) return false
        return APP_NAME_PATTERN.matcher(name).matches()
    }

    fun isPathWithinRoot(targetFile: File, rootDirectory: File): Boolean {
        return try {
            val canonicalTarget = targetFile.canonicalPath
            val canonicalRoot = rootDirectory.canonicalPath
            canonicalTarget.startsWith(canonicalRoot)
        } catch (_: Exception) {
            false
        }
    }

    fun containsTraversal(path: String): Boolean {
        return path.contains("../") || path.contains("..\\") || path.startsWith("..")
    }

    private fun isJavaKeyword(word: String): Boolean {
        val keywords = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while"
        )
        return word in keywords
    }
}
