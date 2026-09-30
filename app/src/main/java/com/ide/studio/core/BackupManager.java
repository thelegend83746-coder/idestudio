package com.ide.studio.core;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Backup Manager: Handles local project snapshots and undo capabilities
 * before AI modifications or destructive operations.
 */
public class BackupManager {

    private static final String BACKUP_DIR_NAME = ".build_studio/backups";

    public static File getBackupDirectory(File projectRoot) {
        File dir = new File(projectRoot, BACKUP_DIR_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Creates a full snapshot backup of the current project app source and res.
     */
    public static File createSnapshot(File projectRoot, String reason) {
        if (projectRoot == null || !projectRoot.exists()) return null;

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File backupsDir = getBackupDirectory(projectRoot);
        File snapshotDir = new File(backupsDir, "snapshot_" + timestamp);
        snapshotDir.mkdirs();

        try {
            // Backup app directory (src, build.gradle, manifests)
            File appDir = new File(projectRoot, "app");
            if (appDir.exists()) {
                File destApp = new File(snapshotDir, "app");
                FileUtils.copyDirectory(appDir, destApp);
            }

            // Save snapshot info
            File infoFile = new File(snapshotDir, "snapshot_info.txt");
            String info = "Reason: " + reason + "\nTimestamp: " + new Date().toString() + "\n";
            FileUtils.writeFile(infoFile, info);

            return snapshotDir;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Lists existing snapshots sorted from newest to oldest.
     */
    public static List<File> listSnapshots(File projectRoot) {
        File backupsDir = getBackupDirectory(projectRoot);
        File[] files = backupsDir.listFiles();
        List<File> result = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory() && f.getName().startsWith("snapshot_")) {
                    result.add(f);
                }
            }
        }
        Collections.sort(result, (a, b) -> b.getName().compareTo(a.getName()));
        return result;
    }

    /**
     * Restores the latest available snapshot backup.
     */
    public static boolean restoreLatestSnapshot(File projectRoot) {
        List<File> snapshots = listSnapshots(projectRoot);
        if (snapshots.isEmpty()) return false;
        return restoreSnapshot(projectRoot, snapshots.get(0));
    }

    /**
     * Restores a specific snapshot.
     */
    public static boolean restoreSnapshot(File projectRoot, File snapshotDir) {
        if (projectRoot == null || snapshotDir == null || !snapshotDir.exists()) return false;

        try {
            File snapApp = new File(snapshotDir, "app");
            if (snapApp.exists()) {
                File currentApp = new File(projectRoot, "app");
                FileUtils.deleteDirectory(currentApp);
                FileUtils.copyDirectory(snapApp, currentApp);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
