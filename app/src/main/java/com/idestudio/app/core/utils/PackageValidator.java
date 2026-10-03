package com.idestudio.app.core.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class PackageValidator {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new",
            "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "true", "false", "null"
    ));

    private PackageValidator() {}

    /**
     * Converts an App Name to a package-compatible segment.
     * e.g. "My Cool App" -> "com.mycoolapp"
     */
    public static String generateDefaultPackage(String appName) {
        if (appName == null || appName.trim().isEmpty()) {
            return "com.example.app";
        }
        String clean = appName.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (clean.isEmpty() || Character.isDigit(clean.charAt(0))) {
            clean = "app" + clean;
        }
        return "com." + clean;
    }

    /**
     * Validates that the package name is a valid Android package name.
     * Must have at least 2 segments separated by dots.
     * Each segment must be a valid Java identifier and not a reserved keyword.
     */
    public static boolean isValidPackageName(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            return false;
        }

        String[] parts = packageName.split("\\.");
        if (parts.length < 2) {
            return false;
        }

        for (String part : parts) {
            if (part.isEmpty()) {
                return false;
            }
            if (!IDENTIFIER_PATTERN.matcher(part).matches()) {
                return false;
            }
            if (JAVA_KEYWORDS.contains(part.toLowerCase())) {
                return false;
            }
        }

        return true;
    }
}
