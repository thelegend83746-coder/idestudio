package com.ide.studio.core;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtils {

    public static String readFile(File file) {
        StringBuilder sb = new StringBuilder();
        if (file == null || !file.exists()) return "";
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static boolean writeFile(File file, String content) {
        try {
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(content != null ? content : "");
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
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

    public static boolean deleteDirectory(File dir) {
        return deleteRecursive(dir);
    }

    public static boolean zipDirectory(File sourceDir, File zipFile) {
        try {
            if (zipFile.getParentFile() != null) {
                zipFile.getParentFile().mkdirs();
            }
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                zipFolderRecursive(sourceDir, sourceDir, zos);
            }
            return true;
        } catch (Exception e) {
            return false;
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

    public static void copyFile(File src, File dest) throws IOException {
        if (dest.getParentFile() != null) {
            dest.getParentFile().mkdirs();
        }
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    public static void copyDirectory(File srcDir, File destDir) throws IOException {
        if (srcDir.isDirectory()) {
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            String[] children = srcDir.list();
            if (children != null) {
                for (String child : children) {
                    copyDirectory(new File(srcDir, child), new File(destDir, child));
                }
            }
        } else {
            copyFile(srcDir, destDir);
        }
    }
}
