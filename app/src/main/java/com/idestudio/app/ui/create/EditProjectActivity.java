package com.idestudio.app.ui.create;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
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
import androidx.core.content.ContextCompat;

import com.idestudio.app.R;
import com.idestudio.app.domain.project.LocalProjectStore;
import com.idestudio.app.domain.project.ProjectMeta;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Activity allowing users to edit an existing project:
 * - App Logo / Icon (Gallery image or Presets)
 * - Application Name
 * - Package Name (applicationId)
 * - Minimum SDK version
 * - Target SDK version
 * Automatically updates project configuration files and Java package paths.
 */
public class EditProjectActivity extends AppCompatActivity {

    private static final String TAG = "EditProjectActivity";
    public static final String EXTRA_PROJECT_ID = "extra_project_id";
    private static final int RC_PICK_IMAGE = 1001;

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
    private LinearLayout btnMinSdk;
    private TextView tvMinSdkValue;
    private LinearLayout btnTargetSdk;
    private TextView tvTargetSdkValue;
    private TextView tvProjectPath;
    private Button btnCancel;
    private Button btnSave;

    private ProjectMeta project;
    private int selectedMinSdk = 21;
    private int selectedTargetSdk = 34;
    private Uri customIconUri = null;
    private int selectedPresetResId = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_project);

        initViews();
        loadProjectData();
        setupListeners();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_edit_project);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Project");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        imgAppIcon = findViewById(R.id.img_edit_app_icon);
        tvIconHint = findViewById(R.id.tv_edit_icon_hint);
        etAppName = findViewById(R.id.et_edit_app_name);
        etPackageName = findViewById(R.id.et_edit_package_name);
        tvPackageError = findViewById(R.id.tv_edit_package_error);
        btnMinSdk = findViewById(R.id.btn_edit_min_sdk);
        tvMinSdkValue = findViewById(R.id.tv_edit_min_sdk_value);
        btnTargetSdk = findViewById(R.id.btn_edit_target_sdk);
        tvTargetSdkValue = findViewById(R.id.tv_edit_target_sdk_value);
        tvProjectPath = findViewById(R.id.tv_edit_project_path);
        btnCancel = findViewById(R.id.btn_edit_cancel);
        btnSave = findViewById(R.id.btn_edit_save);

        findViewById(R.id.btn_edit_pick_gallery).setOnClickListener(v -> openGalleryPicker());
        findViewById(R.id.btn_edit_pick_preset).setOnClickListener(v -> openPresetPicker());
    }

    private void loadProjectData() {
        String projectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);
        if (projectId != null) {
            project = LocalProjectStore.getInstance().getProjectById(projectId);
        }

        if (project == null) {
            Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etAppName.setText(project.getName());
        etPackageName.setText(project.getPackageName());
        selectedMinSdk = project.getMinSdkVersion() > 0 ? project.getMinSdkVersion() : 21;
        selectedTargetSdk = project.getTargetSdkVersion() > 0 ? project.getTargetSdkVersion() : 34;

        updateSdkLabels();
        tvProjectPath.setText(project.getProjectPath());

        // Check if project has an existing custom icon
        File iconFile = new File(project.getProjectPath(), "app/src/main/res/drawable/ic_launcher.png");
        if (iconFile.exists()) {
            imgAppIcon.setImageURI(Uri.fromFile(iconFile));
            tvIconHint.setText("Custom Project Icon");
        } else {
            imgAppIcon.setImageResource(R.mipmap.ic_launcher);
            tvIconHint.setText("Default Launcher Icon");
        }
    }

    private void setupListeners() {
        btnMinSdk.setOnClickListener(v -> showSdkPickerDialog("Select Minimum SDK", selectedMinSdk, sdk -> {
            selectedMinSdk = sdk;
            if (selectedMinSdk > selectedTargetSdk) {
                selectedTargetSdk = selectedMinSdk;
            }
            updateSdkLabels();
        }));

        btnTargetSdk.setOnClickListener(v -> showSdkPickerDialog("Select Target SDK", selectedTargetSdk, sdk -> {
            if (sdk < selectedMinSdk) {
                Toast.makeText(this, "Target SDK cannot be less than Minimum SDK", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedTargetSdk = sdk;
            updateSdkLabels();
        }));

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void updateSdkLabels() {
        tvMinSdkValue.setText(getSdkLabel(selectedMinSdk));
        tvTargetSdkValue.setText(getSdkLabel(selectedTargetSdk));
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
                R.mipmap.ic_launcher
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

    private void saveChanges() {
        String newName = etAppName.getText().toString().trim();
        String newPackage = etPackageName.getText().toString().trim();

        if (newName.isEmpty()) {
            etAppName.setError("App name cannot be empty");
            return;
        }

        if (newPackage.isEmpty() || !Pattern.matches(PACKAGE_REGEX, newPackage)) {
            tvPackageError.setVisibility(View.VISIBLE);
            tvPackageError.setText("Invalid package name (e.g. com.company.app)");
            return;
        }
        tvPackageError.setVisibility(View.GONE);

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        final String oldPackage = project.getPackageName();
        final String oldName = project.getName();

        new Thread(() -> {
            boolean success = applyProjectModifications(oldName, newName, oldPackage, newPackage, selectedMinSdk, selectedTargetSdk);

            new Handler(Looper.getMainLooper()).post(() -> {
                btnSave.setEnabled(true);
                btnSave.setText("Save Changes");

                if (success) {
                    Toast.makeText(EditProjectActivity.this, "Project updated successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(EditProjectActivity.this, "Failed to update project files.", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private boolean applyProjectModifications(String oldName, String newName, String oldPackage, String newPackage, int minSdk, int targetSdk) {
        try {
            File projectDir = new File(project.getProjectPath());
            if (!projectDir.exists()) {
                return false;
            }

            // 1. Update strings.xml (app_name)
            File stringsFile = new File(projectDir, "app/src/main/res/values/strings.xml");
            if (stringsFile.exists()) {
                String content = readFileToString(stringsFile);
                content = content.replaceAll("<string name=\"app_name\">.*?</string>", "<string name=\"app_name\">" + escapeXml(newName) + "</string>");
                writeStringToFile(stringsFile, content);
            }

            // 2. Update app/build.gradle
            File gradleFile = new File(projectDir, "app/build.gradle");
            if (gradleFile.exists()) {
                String content = readFileToString(gradleFile);
                content = content.replaceAll("applicationId\\s+[\"'].*?[\"']", "applicationId \"" + newPackage + "\"");
                content = content.replaceAll("minSdk(?:Version)?\\s+\\d+", "minSdk " + minSdk);
                content = content.replaceAll("targetSdk(?:Version)?\\s+\\d+", "targetSdk " + targetSdk);
                writeStringToFile(gradleFile, content);
            }

            // 3. Update AndroidManifest.xml
            File manifestFile = new File(projectDir, "app/src/main/AndroidManifest.xml");
            if (manifestFile.exists()) {
                String content = readFileToString(manifestFile);
                content = content.replaceAll("package=[\"'].*?[\"']", "package=\"" + newPackage + "\"");
                // Ensure android:icon is set to @drawable/ic_launcher
                if (!content.contains("android:icon=")) {
                    content = content.replace("<application", "<application\n        android:icon=\"@drawable/ic_launcher\"");
                }
                writeStringToFile(manifestFile, content);
            }

            // 4. Update Java package structure if package name changed
            if (!oldPackage.equals(newPackage)) {
                updateJavaPackageStructure(projectDir, oldPackage, newPackage);
            }

            // 5. Update or save App Icon
            saveProjectIcon(projectDir);

            // 6. Update ProjectMeta in store
            project.setName(newName);
            project.setPackageName(newPackage);
            project.setMinSdkVersion(minSdk);
            project.setTargetSdkVersion(targetSdk);
            project.setLastModified(System.currentTimeMillis());

            LocalProjectStore.getInstance().saveProject(project);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error applying project modifications: " + e.getMessage(), e);
            return false;
        }
    }

    private void updateJavaPackageStructure(File projectDir, String oldPkg, String newPkg) {
        File javaSrcDir = new File(projectDir, "app/src/main/java");
        if (!javaSrcDir.exists()) return;

        // Find all java files
        List<File> javaFiles = new ArrayList<>();
        collectFiles(javaSrcDir, ".java", javaFiles);

        for (File javaFile : javaFiles) {
            try {
                String content = readFileToString(javaFile);
                if (content.contains("package " + oldPkg)) {
                    content = content.replace("package " + oldPkg, "package " + newPkg);
                    writeStringToFile(javaFile, content);
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to update package in " + javaFile.getName(), e);
            }
        }

        // Relocate files into new package directory
        File oldPkgDir = new File(javaSrcDir, oldPkg.replace('.', '/'));
        File newPkgDir = new File(javaSrcDir, newPkg.replace('.', '/'));

        if (oldPkgDir.exists() && !oldPkgDir.equals(newPkgDir)) {
            newPkgDir.mkdirs();
            File[] files = oldPkgDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        File dest = new File(newPkgDir, file.getName());
                        file.renameTo(dest);
                    }
                }
            }
            // Clean up empty old dirs
            deleteEmptyParents(oldPkgDir, javaSrcDir);
        }
    }

    private void saveProjectIcon(File projectDir) {
        try {
            File resDrawableDir = new File(projectDir, "app/src/main/res/drawable");
            if (!resDrawableDir.exists()) resDrawableDir.mkdirs();
            File targetIconFile = new File(resDrawableDir, "ic_launcher.png");

            Bitmap bitmap = null;

            if (customIconUri != null) {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), customIconUri);
            } else if (selectedPresetResId != 0) {
                Drawable drawable = ContextCompat.getDrawable(this, selectedPresetResId);
                if (drawable != null) {
                    bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawable.draw(canvas);
                }
            }

            if (bitmap != null) {
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 192, 192, true);
                try (FileOutputStream fos = new FileOutputStream(targetIconFile)) {
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                }
                Log.i(TAG, "Saved new project launcher icon: " + targetIconFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to save project icon: " + e.getMessage(), e);
        }
    }

    private void collectFiles(File dir, String extension, List<File> result) {
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) {
                collectFiles(f, extension, result);
            } else if (f.getName().endsWith(extension)) {
                result.add(f);
            }
        }
    }

    private void deleteEmptyParents(File dir, File stopAt) {
        if (dir == null || dir.equals(stopAt)) return;
        File[] children = dir.listFiles();
        if (children == null || children.length == 0) {
            File parent = dir.getParentFile();
            dir.delete();
            deleteEmptyParents(parent, stopAt);
        }
    }

    private String readFileToString(File file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private void writeStringToFile(File file, String content) throws Exception {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content);
            writer.flush();
        }
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "\\'");
    }
}
