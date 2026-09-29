package com.ide.studio;

import android.app.Application;
import android.content.Context;
import com.ide.studio.core.PreferencesManager;
import com.ide.studio.core.ProjectStorage;
import com.ide.studio.compiler.ToolchainValidator;
import java.util.concurrent.Executors;

/**
 * Main application class for idestudio.
 * Manages global singleton state, preferences initialization,
 * and background asset extraction for the offline compiler.
 */
public class IdeStudioApp extends Application {

    private static IdeStudioApp sInstance;
    private PreferencesManager mPreferencesManager;
    private ProjectStorage mProjectStorage;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        mPreferencesManager = new PreferencesManager(this);
        mProjectStorage = new ProjectStorage(this);

        // Pre-warm toolchain checks and asset verification asynchronously
        Executors.newSingleThreadExecutor().execute(() -> {
            ToolchainValidator.ValidationResult result = ToolchainValidator.validate(this);
            // Result is cached for quick inspection
        });
    }

    public static IdeStudioApp getInstance() {
        return sInstance;
    }

    public static Context getAppContext() {
        return sInstance.getApplicationContext();
    }

    public PreferencesManager getPreferencesManager() {
        return mPreferencesManager;
    }

    public ProjectStorage getProjectStorage() {
        return mProjectStorage;
    }
}
