package com.ide.studio;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.Formatter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.ide.studio.core.PreferencesManager;
import java.io.File;

/**
 * Application Settings:
 * - Dark Mode switch
 * - Confirm Before Delete switch
 * - Clear App Cache with size display & confirmation dialog
 */
public class ApplicationSettingsActivity extends AppCompatActivity {

    private PreferencesManager mPrefs;
    private MaterialSwitch mSwitchDarkMode;
    private MaterialSwitch mSwitchConfirmDelete;
    private LinearLayout mRowClearCache;
    private TextView mTvCacheInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application_settings);

        mPrefs = new PreferencesManager(this);

        ImageView btnBack = findViewById(R.id.btn_back);
        mSwitchDarkMode = findViewById(R.id.switch_dark_mode);
        mSwitchConfirmDelete = findViewById(R.id.switch_confirm_delete);
        mRowClearCache = findViewById(R.id.row_clear_cache);
        mTvCacheInfo = findViewById(R.id.tv_cache_info);

        btnBack.setOnClickListener(v -> finish());

        // Initialize Dark Mode switch
        mSwitchDarkMode.setChecked(mPrefs.isDarkMode());
        mSwitchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.setDarkMode(isChecked);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Initialize Confirm Delete switch
        mSwitchConfirmDelete.setChecked(mPrefs.isConfirmBeforeDelete());
        mSwitchConfirmDelete.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.setConfirmBeforeDelete(isChecked);
        });

        // Clear Cache action
        mRowClearCache.setOnClickListener(v -> promptClearCache());

        updateCacheDisplay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCacheDisplay();
    }

    private void updateCacheDisplay() {
        long totalCache = getDirSize(getCacheDir()) + getDirSize(getExternalCacheDir());
        String sizeFormatted = Formatter.formatFileSize(this, totalCache);
        mTvCacheInfo.setText("Free up temporary build cache files (" + sizeFormatted + ")");
    }

    private void promptClearCache() {
        new AlertDialog.Builder(this)
                .setTitle("Clear App Cache")
                .setMessage("Are you sure you want to clear temporary build and app cache files? Project source files will not be affected.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    deleteDir(getCacheDir());
                    deleteDir(getExternalCacheDir());
                    updateCacheDisplay();
                    Toast.makeText(this, "App cache cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private long getDirSize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    size += getDirSize(file);
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        }
        return false;
    }
}
