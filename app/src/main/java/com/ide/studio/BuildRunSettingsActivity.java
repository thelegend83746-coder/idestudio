package com.ide.studio;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.ide.studio.compiler.ToolchainValidator;
import com.ide.studio.core.PreferencesManager;

/**
 * Build & Run Settings: Target SDK platform selection, toolchain diagnostics, APK signing options.
 */
public class BuildRunSettingsActivity extends AppCompatActivity {

    private PreferencesManager mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build_run_settings);

        mPrefs = new PreferencesManager(this);

        ImageView btnBack = findViewById(R.id.btn_back);
        Spinner spinnerPlatform = findViewById(R.id.spinner_platform);
        TextView tvToolchainStatus = findViewById(R.id.tv_toolchain_status);
        Spinner spinnerSigning = findViewById(R.id.spinner_signing);
        CheckBox cbVerbose = findViewById(R.id.cb_verbose_logging);
        Button btnSave = findViewById(R.id.btn_save_build_settings);

        btnBack.setOnClickListener(v -> finish());

        // Platform SDK options (API 30 to 36)
        String[] platforms = new String[]{
            "Android 13 (API 33 - Recommended)",
            "Android 14 (API 34)",
            "Android 15 (API 35)",
            "Android 16 (API 36)",
            "Android 12L (API 32)",
            "Android 12 (API 31)",
            "Android 11 (API 30)"
        };
        ArrayAdapter<String> platAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, platforms);
        spinnerPlatform.setAdapter(platAdapter);

        // Toolchain live check
        ToolchainValidator.ValidationResult result = ToolchainValidator.validate(this);
        if (result.isValid) {
            tvToolchainStatus.setText("[AAPT2]: Ready\n[ECJ]: Ready\n[D8 Dexer]: Ready\n[ApkSigner]: Ready\n[All Tools Available]");
        } else {
            StringBuilder sb = new StringBuilder();
            for (String err : result.errors) {
                sb.append(err).append("\n");
            }
            tvToolchainStatus.setText(sb.toString().trim());
        }

        // Signing options
        String[] signing = new String[]{"Built-in Testkey (v1 + v2 + v3)", "Custom Keystore"};
        ArrayAdapter<String> signAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, signing);
        spinnerSigning.setAdapter(signAdapter);

        cbVerbose.setChecked(mPrefs.isShowBuildLogs());

        btnSave.setOnClickListener(v -> {
            mPrefs.setShowBuildLogs(cbVerbose.isChecked());
            Toast.makeText(this, "Build settings saved!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
