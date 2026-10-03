package com.idestudio.app.core.utils;

import java.io.File;
import java.io.IOException;

public final class PathSanitizer {
    private PathSanitizer() {}

    /**
     * Ensures that the target file resides strictly within the allowed root directory
     * and does not attempt path traversal (e.g., ../ attacks).
     */
    public static boolean isPathSafe(File rootDir, File targetFile) {
        try {
            String canonicalRoot = rootDir.getCanonicalPath();
            String canonicalTarget = targetFile.getCanonicalPath();
            return canonicalTarget.startsWith(canonicalRoot);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Sanitizes a file or folder name to prevent illegal characters in filesystems.
     */
    public static String sanitizeFileName(String inputName) {
        if (inputName == null) return "";
        return inputName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
