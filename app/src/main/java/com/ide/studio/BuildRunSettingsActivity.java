package com.ide.studio;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.ide.studio.compiler.ToolchainValidator;
import com.ide.studio.core.PreferencesManager;
import com.ide.studio.core.ProjectStorage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Build & Run Settings:
 * - Default Minimum SDK
 * - Default Target SDK (with live local SDK availability validation)
 * - Active Toolchain Diagnostics (AAPT2, ECJ, D8, ApkSigner)
 * - Auto-save before build
 * - Real-time build stdout/stderr output dialog toggle
 * - Clean intermediate build cache
 */
public class BuildRunSettingsActivity extends AppCompatActivity {

    private PreferencesManager mPrefs;
    private EditText mEtDefaultMinSdk;
    private Spinner mSpinnerTargetSdk;
    private TextView mTvSdkStatus;
    private TextView mTvDiagnostics;
    private MaterialSwitch mSwitchAutoSave;
    private MaterialSwitch mSwitchShowLogs;
    private LinearLayout mRowClearBuildCache;
    private Button mBtnSave;

    private static class SdkItem {
        final int api;
        final String label;

        SdkItem(int api, String label) {
            this.api = api;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final List<SdkItem> mSdkItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build_run_settings);

        mPrefs = new PreferencesManager(this);

        ImageView btnBack = findViewById(R.id.btn_back);
        mEtDefaultMinSdk = findViewById(R.id.et_default_min_sdk);
        mSpinnerTargetSdk = findViewById(R.id.spinner_target_sdk);
        mTvSdkStatus = findViewById(R.id.tv_sdk_status);
        mTvDiagnostics = findViewById(R.id.tv_toolchain_diagnostics);
        mSwitchAutoSave = findViewById(R.id.switch_auto_save);
        mSwitchShowLogs = findViewById(R.id.switch_show_logs);
        mRowClearBuildCache = findViewById(R.id.row_clear_build_cache);
        mBtnSave = findViewById(R.id.btn_save_build_settings);

        btnBack.setOnClickListener(v -> finish());

        // SDK items list
        mSdkItems.add(new SdkItem(35, "API 35 (Android 15)"));
        mSdkItems.add(new SdkItem(34, "API 34 (Android 14)"));
        mSdkItems.add(new SdkItem(33, "API 33 (Android 13)"));
        mSdkItems.add(new SdkItem(32, "API 32 (Android 12L)"));
        mSdkItems.add(new SdkItem(31, "API 31 (Android 12)"));
        mSdkItems.add(new SdkItem(30, "API 30 (Android 11)"));
        mSdkItems.add(new SdkItem(29, "API 29 (Android 10)"));
        mSdkItems.add(new SdkItem(28, "API 28 (Android 9.0)"));
        mSdkItems.add(new SdkItem(26, "API 26 (Android 8.0)"));
        mSdkItems.add(new SdkItem(24, "API 24 (Android 7.0)"));
        mSdkItems.add(new SdkItem(21, "API 21 (Android 5.0)"));

        ArrayAdapter<SdkItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mSdkItems);
        mSpinnerTargetSdk.setAdapter(adapter);

        // Preselect current configured default target SDK
        int currentTarget = mPrefs.getDefaultTargetSdk();
        int selectedIndex = 0;
        for (int i = 0; i < mSdkItems.size(); i++) {
            if (mSdkItems.get(i).api == currentTarget) {
                selectedIndex = i;
                break;
            }
        }
        mSpinnerTargetSdk.setSelection(selectedIndex);

        mSpinnerTargetSdk.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SdkItem selected = mSdkItems.get(position);
                updateSdkStatus(selected.api);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Min SDK
        mEtDefaultMinSdk.setText(String.valueOf(mPrefs.getDefaultMinSdk()));

        // Toolchain live check
        ToolchainValidator.ValidationResult result = ToolchainValidator.validate(this);
        if (result.isValid) {
            mTvDiagnostics.setText("[AAPT2]: Ready (Native arm64/v7a/x86)\n[ECJ 4.6.1]: Ready\n[D8 Dexer]: Ready\n[ApkSigner]: Ready\n[Signing Key]: testkey.pk8 Verified");
        } else {
            StringBuilder sb = new StringBuilder();
            for (String err : result.errors) {
                sb.append(err).append("\n");
            }
            mTvDiagnostics.setText(sb.toString().trim());
        }

        mSwitchAutoSave.setChecked(mPrefs.isAutoSaveBeforeBuild());
        mSwitchShowLogs.setChecked(mPrefs.isShowBuildLogs());

        mRowClearBuildCache.setOnClickListener(v -> promptClearBuildCache());

        mBtnSave.setOnClickListener(v -> saveSettings());
    }

    private void updateSdkStatus(int apiLevel) {
        boolean available = ToolchainValidator.isSdkAvailable(this, apiLevel);
        if (available) {
            mTvSdkStatus.setText("Installed & Ready (API " + apiLevel + ")");
            mTvSdkStatus.setTextColor(Color.parseColor("#10B981"));
        } else {
            mTvSdkStatus.setText("SDK not installed / unavailable");
            mTvSdkStatus.setTextColor(Color.parseColor("#EF4444"));
        }
    }

    private void promptClearBuildCache() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Build Cache")
                .setMessage("Are you sure you want to clean all intermediate compiler caches (DEX stubs, AAPT2 flatted archives)?")
                .setPositiveButton("Clean", (dialog, which) -> {
                    File buildCache = new File(getFilesDir(), "build_cache");
                    deleteRecursive(buildCache);
                    File projectsDir = ProjectStorage.getProjectsRoot();
                    cleanProjectBuildDirs(projectsDir);
                    Toast.makeText(this, "Intermediate build cache cleaned", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void cleanProjectBuildDirs(File root) {
        if (root == null || !root.exists()) return;
        File[] files = root.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                File bin = new File(f, "app/build");
                if (bin.exists()) {
                    deleteRecursive(bin);
                }
            }
        }
    }

    private void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }

    private void saveSettings() {
        try {
            int minSdk = Integer.parseInt(mEtDefaultMinSdk.getText().toString().trim());
            SdkItem selectedItem = (SdkItem) mSpinnerTargetSdk.getSelectedItem();
            int targetSdk = selectedItem != null ? selectedItem.api : 33;

            if (minSdk < 14 || minSdk > 36) {
                Toast.makeText(this, "Minimum SDK must be between 14 and 36", Toast.LENGTH_SHORT).show();
                return;
            }
            if (targetSdk < minSdk) {
                Toast.makeText(this, "Target SDK must be >= Minimum SDK", Toast.LENGTH_SHORT).show();
                return;
            }

            mPrefs.setDefaultMinSdk(minSdk);
            mPrefs.setDefaultTargetSdk(targetSdk);
            mPrefs.setAutoSaveBeforeBuild(mSwitchAutoSave.isChecked());
            mPrefs.setShowBuildLogs(mSwitchShowLogs.isChecked());

            Toast.makeText(this, "Build settings saved", Toast.LENGTH_SHORT).show();
            finish();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid Minimum SDK number", Toast.LENGTH_SHORT).show();
        }
    }
}
