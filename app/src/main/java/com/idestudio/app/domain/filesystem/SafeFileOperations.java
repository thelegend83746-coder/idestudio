package com.idestudio.app.domain.filesystem;

import com.idestudio.app.core.utils.PathSanitizer;

import java.io.File;
import java.io.IOException;

public final class SafeFileOperations {

    private SafeFileOperations() {}

    public static File createFile(File projectRoot, File parentDir, String fileName) throws IOException {
        String sanitizedName = PathSanitizer.sanitizeFileName(fileName);
        if (sanitizedName.isEmpty()) {
            throw new IllegalArgumentException("Invalid file name");
        }

        File target = new File(parentDir, sanitizedName);
        if (!PathSanitizer.isPathSafe(projectRoot, target)) {
            throw new SecurityException("Path traversal attempt detected!");
        }

        if (target.exists()) {
            throw new IOException("File already exists: " + sanitizedName);
        }

        if (!target.createNewFile()) {
            throw new IOException("Failed to create file: " + sanitizedName);
        }

        return target;
    }

    public static File createFolder(File projectRoot, File parentDir, String folderName) throws IOException {
        String sanitizedName = PathSanitizer.sanitizeFileName(folderName);
        if (sanitizedName.isEmpty()) {
            throw new IllegalArgumentException("Invalid folder name");
        }

        File target = new File(parentDir, sanitizedName);
        if (!PathSanitizer.isPathSafe(projectRoot, target)) {
            throw new SecurityException("Path traversal attempt detected!");
        }

        if (target.exists()) {
            throw new IOException("Folder already exists: " + sanitizedName);
        }

        if (!target.mkdirs()) {
            throw new IOException("Failed to create folder: " + sanitizedName);
        }

        return target;
    }

    public static File rename(File projectRoot, File target, String newName) throws IOException {
        String sanitizedName = PathSanitizer.sanitizeFileName(newName);
        if (sanitizedName.isEmpty()) {
            throw new IllegalArgumentException("Invalid new name");
        }

        File destination = new File(target.getParentFile(), sanitizedName);
        if (!PathSanitizer.isPathSafe(projectRoot, destination)) {
            throw new SecurityException("Path traversal attempt detected!");
        }

        if (destination.exists()) {
            throw new IOException("Target name already exists: " + sanitizedName);
        }

        if (!target.renameTo(destination)) {
            throw new IOException("Failed to rename to: " + sanitizedName);
        }

        return destination;
    }

    public static boolean delete(File projectRoot, File target) throws IOException {
        if (!PathSanitizer.isPathSafe(projectRoot, target)) {
            throw new SecurityException("Path traversal attempt detected!");
        }

        return deleteRecursive(target);
    }

    private static boolean deleteRecursive(File fileOrDir) {
        if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return fileOrDir.delete();
    }
}
