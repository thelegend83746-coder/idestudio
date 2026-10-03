package com.idestudio.app.ui.create;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.idestudio.app.R;
import com.idestudio.app.domain.project.LocalProjectStore;
import com.idestudio.app.domain.project.ProjectGenerator;
import com.idestudio.app.ui.editor.EditorActivity;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Screen for creating a new Android project.
 * Automatically saves new projects inside /storage/emulated/0/idestudio/<ProjectName>
 */
public class ConfigureProjectActivity extends AppCompatActivity {

    private static final int RC_PICK_IMAGE = 1002;
    private static final String PACKAGE_REGEX = "^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$";

    private static final int[] SDK_VERSIONS = {21, 23, 24, 26, 28, 29, 30, 31, 33, 34};
    private static final String[] SDK_NAMES = {
            "API 21: Android 5.0 (Lollipop)",
            "API 23: Android 6.0 (Marshmallow)",
            "API 24: Android 7.0 (Nougat)",
            "API 26: Android 8.0 (Oreo)",
            "API 28: Android 9.0 (Pie)",
            "API 29: Android 10",
            "API 30: Android 11",
            "API 31: Android 12",
            "API 33: Android 13",
            "API 34: Android 14"
    };

    private ImageView imgAppIcon;
    private TextView tvIconHint;
    private EditText etAppName;
    private EditText etPackageName;
    private TextView tvPackageError;
    private TextView tvProjectPath;
    private LinearLayout btnMinSdk;
    private TextView tvMinSdkValue;
    private LinearLayout btnTargetSdk;
    private TextView tvTargetSdkValue;
    private Button btnCreate;

    private int selectedMinSdk = 21;
    private int selectedTargetSdk = 34;
    private Uri customIconUri = null;
    private int selectedPresetResId = 0;
    private String templateType = "Empty Activity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_project);

        initViews();
        setupListeners();
        updatePathPreview();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_configure);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("New Android Project");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        imgAppIcon = findViewById(R.id.img_configure_icon);
        tvIconHint = findViewById(R.id.tv_configure_icon_hint);
        etAppName = findViewById(R.id.et_configure_app_name);
        etPackageName = findViewById(R.id.et_configure_package_name);
        tvPackageError = findViewById(R.id.tv_configure_package_error);
        tvProjectPath = findViewById(R.id.tv_configure_project_path);
        btnMinSdk = findViewById(R.id.btn_configure_min_sdk);
        tvMinSdkValue = findViewById(R.id.tv_configure_min_sdk_value);
        btnTargetSdk = findViewById(R.id.btn_configure_target_sdk);
        tvTargetSdkValue = findViewById(R.id.tv_configure_target_sdk_value);
        btnCreate = findViewById(R.id.btn_configure_create);

        findViewById(R.id.btn_configure_pick_gallery).setOnClickListener(v -> openGalleryPicker());
        findViewById(R.id.btn_configure_pick_preset).setOnClickListener(v -> openPresetPicker());

        tvMinSdkValue.setText(getSdkLabel(selectedMinSdk));
        tvTargetSdkValue.setText(getSdkLabel(selectedTargetSdk));
    }

    private void setupListeners() {
        etAppName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String name = s.toString().trim();
                String sanitized = name.toLowerCase().replaceAll("[^a-z0-9]", "");
                if (sanitized.isEmpty()) sanitized = "myapp";
                etPackageName.setHint("com.example." + sanitized);
                updatePathPreview();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnMinSdk.setOnClickListener(v -> showSdkPickerDialog("Select Minimum SDK", selectedMinSdk, sdk -> {
            selectedMinSdk = sdk;
            if (selectedMinSdk > selectedTargetSdk) {
                selectedTargetSdk = selectedMinSdk;
            }
            tvMinSdkValue.setText(getSdkLabel(selectedMinSdk));
            tvTargetSdkValue.setText(getSdkLabel(selectedTargetSdk));
        }));

        btnTargetSdk.setOnClickListener(v -> showSdkPickerDialog("Select Target SDK", selectedTargetSdk, sdk -> {
            if (sdk < selectedMinSdk) {
                Toast.makeText(this, "Target SDK cannot be less than Minimum SDK", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedTargetSdk = sdk;
            tvTargetSdkValue.setText(getSdkLabel(selectedTargetSdk));
        }));

        btnCreate.setOnClickListener(v -> createProject());
    }

    private void updatePathPreview() {
        String name = etAppName.getText().toString().trim();
        if (name.isEmpty()) name = "MyApplication";
        String basePath = LocalProjectStore.getBaseDirPath();
        tvProjectPath.setText(basePath + "/" + name);
    }

    private String getSdkLabel(int api) {
        for (int i = 0; i < SDK_VERSIONS.length; i++) {
            if (SDK_VERSIONS[i] == api) {
                return SDK_NAMES[i];
            }
        }
        return "API " + api;
    }

    private interface SdkSelectListener {
        void onSelected(int sdk);
    }

    private void showSdkPickerDialog(String title, int currentSdk, SdkSelectListener listener) {
        int selectedIndex = 0;
        for (int i = 0; i < SDK_VERSIONS.length; i++) {
            if (SDK_VERSIONS[i] == currentSdk) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(SDK_NAMES, selectedIndex, (dialog, which) -> {
                    listener.onSelected(SDK_VERSIONS[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select App Icon"), RC_PICK_IMAGE);
    }

    private void openPresetPicker() {
        String[] presets = {"Game Console", "Code & Dev", "Rocket Launch", "Default Android"};
        int[] drawables = {
                R.drawable.ic_preset_game,
                R.drawable.ic_preset_code,
                R.drawable.ic_preset_rocket,
                R.drawable.ic_launcher
        };

        new AlertDialog.Builder(this)
                .setTitle("Choose Preset Icon")
                .setItems(presets, (dialog, which) -> {
                    selectedPresetResId = drawables[which];
                    customIconUri = null;
                    imgAppIcon.setImageResource(selectedPresetResId);
                    tvIconHint.setText("Preset: " + presets[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            customIconUri = data.getData();
            selectedPresetResId = 0;
            imgAppIcon.setImageURI(customIconUri);
            tvIconHint.setText("Custom Gallery Image");
        }
    }

    private void createProject() {
        String name = etAppName.getText().toString().trim();
        String pkg = etPackageName.getText().toString().trim();

        if (name.isEmpty()) {
            etAppName.setError("Please enter an application name");
            return;
        }

        if (pkg.isEmpty()) {
            String sanitized = name.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (sanitized.isEmpty()) sanitized = "myapp";
            pkg = "com.example." + sanitized;
        }

        if (!Pattern.matches(PACKAGE_REGEX, pkg)) {
            tvPackageError.setVisibility(View.VISIBLE);
            tvPackageError.setText("Invalid package name (e.g. com.company.app)");
            return;
        }
        tvPackageError.setVisibility(View.GONE);

        btnCreate.setEnabled(false);
        btnCreate.setText("Creating Project...");

        final String finalName = name;
        final String finalPkg = pkg;

        new Thread(() -> {
            try {
                File projectDir = ProjectGenerator.createProject(
                        ConfigureProjectActivity.this,
                        finalName,
                        finalPkg,
                        templateType,
                        selectedMinSdk,
                        selectedTargetSdk,
                        customIconUri,
                        selectedPresetResId
                );

                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(ConfigureProjectActivity.this, "Project created in /storage/emulated/0/idestudio!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ConfigureProjectActivity.this, EditorActivity.class);
                    intent.putExtra("project_name", finalName);
                    intent.putExtra("project_path", projectDir.getAbsolutePath());
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    btnCreate.setEnabled(true);
                    btnCreate.setText("Create Project");
                    Toast.makeText(ConfigureProjectActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
