package com.idestudio.app.domain.project;

import com.idestudio.app.data.models.ProjectMeta;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Handles exporting projects into standalone .zip backup archives.
 * Backups are stored in /storage/emulated/0/idestudio/backups/
 */
public class ProjectExporter {

    private static final String TAG = "ProjectExporter";

    public interface ExportCallback {
        void onSuccess(File zipFile);
        void onError(String errorMessage);
    }

    public static File getBackupDirectory() {
        File base = LocalProjectStore.getResolvedBaseDir();
        File backupDir = new File(base, "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        return backupDir;
    }

    /**
     * Synchronously exports a project to a zip file.
     */
    public static File exportProjectToZip(ProjectMeta project) throws IOException {
        if (project == null || project.getProjectPath() == null) {
            throw new IllegalArgumentException("Invalid project or project path");
        }

        File projectDir = new File(project.getProjectPath());
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            throw new IOException("Project directory does not exist: " + project.getProjectPath());
        }

        File backupDir = getBackupDirectory();

        String safeName = project.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String zipFileName = safeName + "_backup_" + timestamp + ".zip";
        File targetZipFile = new File(backupDir, zipFileName);

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(targetZipFile)))) {
            zipDirectory(projectDir, projectDir.getAbsolutePath(), zos);
            zos.flush();
        } catch (Exception e) {
            Log.e(TAG, "Failed to zip project: " + e.getMessage(), e);
            if (targetZipFile.exists()) {
                targetZipFile.delete();
            }
            throw new IOException("Failed to export zip: " + e.getMessage(), e);
        }

        Log.i(TAG, "Successfully exported project backup to: " + targetZipFile.getAbsolutePath());
        return targetZipFile;
    }

    /**
     * Asynchronously exports a project and notifies the callback.
     */
    public static void exportProjectAsync(ProjectMeta project, ExportCallback callback) {
        new Thread(() -> {
            try {
                File zip = exportProjectToZip(project);
                if (callback != null) {
                    callback.onSuccess(zip);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    private static void zipDirectory(File currentFile, String rootPath, ZipOutputStream zos) throws IOException {
        if (currentFile.isHidden()) {
            if (!currentFile.getName().equals(".metadata")) {
                return;
            }
        }

        if (currentFile.isDirectory()) {
            if (currentFile.getName().equals("build") || currentFile.getName().equals(".gradle")) {
                return;
            }

            File[] children = currentFile.listFiles();
            if (children != null) {
                for (File child : children) {
                    zipDirectory(child, rootPath, zos);
                }
            }
        } else {
            String relativePath = currentFile.getAbsolutePath().substring(rootPath.length());
            if (relativePath.startsWith(File.separator)) {
                relativePath = relativePath.substring(1);
            }
            relativePath = relativePath.replace("\\", "/");

            ZipEntry entry = new ZipEntry(relativePath);
            entry.setTime(currentFile.lastModified());
            zos.putNextEntry(entry);

            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(currentFile))) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = bis.read(buffer)) != -1) {
                    zos.write(buffer, 0, count);
                }
            }
            zos.closeEntry();
        }
    }
}
