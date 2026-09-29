package com.ide.studio.core;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtils {

    public static String readFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    public static void writeFile(File file, String content) throws IOException {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        }
    }

    public static boolean deleteRecursive(File fileOrDir) {
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

    public static void zipDirectory(File sourceDir, File zipFile) throws IOException {
        if (zipFile.getParentFile() != null) {
            zipFile.getParentFile().mkdirs();
        }
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipFolderRecursive(sourceDir, sourceDir, zos);
        }
    }

    private static void zipFolderRecursive(File rootDir, File currentFile, ZipOutputStream zos) throws IOException {
        if (currentFile.isDirectory()) {
            File[] files = currentFile.listFiles();
            if (files != null) {
                for (File f : files) {
                    zipFolderRecursive(rootDir, f, zos);
                }
            }
        } else {
            String relPath = rootDir.toURI().relativize(currentFile.toURI()).getPath();
            ZipEntry entry = new ZipEntry(relPath);
            zos.putNextEntry(entry);
            try (FileInputStream fis = new FileInputStream(currentFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
            }
            zos.closeEntry();
        }
    }
}
