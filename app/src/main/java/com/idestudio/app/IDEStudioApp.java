package com.idestudio.app;

import android.app.Application;
import android.util.Log;

import com.idestudio.app.domain.project.LocalProjectStore;

import java.io.File;

/**
 * Application entry point for IDE Studio.
 * Ensures the root workspace folder /storage/emulated/0/idestudio is initialized.
 */
public class IDEStudioApp extends Application {

    private static final String TAG = "IDEStudioApp";
    private static IDEStudioApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        initWorkspace();
    }

    public static IDEStudioApp getInstance() {
        return instance;
    }

    private void initWorkspace() {
        try {
            File baseDir = LocalProjectStore.getResolvedBaseDir();
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }

            File backupsDir = new File(baseDir, "backups");
            if (!backupsDir.exists()) {
                backupsDir.mkdirs();
            }

            // Initialize LocalProjectStore singleton
            LocalProjectStore.getInstance();
            Log.i(TAG, "Initialized IDE Studio workspace at: " + baseDir.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize workspace: " + e.getMessage(), e);
        }
    }
}
