package com.ide.studio;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.ide.studio.core.PreferencesManager;
import com.ide.studio.core.ProjectStorage;
import com.ide.studio.model.Project;

/**
 * 2-Step Project Creation Wizard.
 * Step 1: Choose Template (No Activity, Empty Activity, Basic Activity, Compose Activity)
 * Step 2: Configure Project (App Icon, App Name, Package, Min/Target SDK, Save Location, Language: JAVA)
 */
public class CreateProjectActivity extends AppCompatActivity {

    private LinearLayout mLayoutStepTemplates;
    private LinearLayout mLayoutStepConfigure;

    // Step 1 Views
    private LinearLayout mCardNoActivity;
    private LinearLayout mCardEmptyActivity;
    private LinearLayout mCardBasicActivity;
    private LinearLayout mCardComposeActivity;
    private TextView mBtnExitTemplates;
    private Button mBtnNextToConfigure;

    // Step 2 Views
    private LinearLayout mRowAppIcon;
    private ImageView mIvProjectLogo;
    private EditText mEtAppName;
    private EditText mEtPackageName;
    private EditText mEtMinSdk;
    private EditText mEtTargetSdk;
    private EditText mEtSaveLocation;
    private EditText mEtLanguage;
    private TextView mBtnPreviousToTemplates;
    private Button mBtnDoneCreateProject;

    private String mSelectedTemplate = "Empty Activity";
    private Bitmap mSelectedIconBitmap = null;
    private boolean mUserEditedPackageName = false;
    private PreferencesManager mPrefs;

    private final ActivityResultLauncher<String> mGetIconLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                            mSelectedIconBitmap = ImageDecoder.decodeBitmap(source);
                        } else {
                            mSelectedIconBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        }
                        mIvProjectLogo.setImageBitmap(mSelectedIconBitmap);
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to load icon", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_project);

        mPrefs = new PreferencesManager(this);

        initViews();
        setupTemplateSelection();
        setupStepNavigation();
        setupInputValidation();
    }

    private void initViews() {
        mLayoutStepTemplates = findViewById(R.id.layout_step_templates);
        mLayoutStepConfigure = findViewById(R.id.layout_step_configure);

        // Step 1
        mCardNoActivity = findViewById(R.id.card_template_no_activity);
        mCardEmptyActivity = findViewById(R.id.card_template_empty_activity);
        mCardBasicActivity = findViewById(R.id.card_template_basic_activity);
        mCardComposeActivity = findViewById(R.id.card_template_compose_activity);
        mBtnExitTemplates = findViewById(R.id.btn_exit_templates);
        mBtnNextToConfigure = findViewById(R.id.btn_next_to_configure);

        // Step 2
        mRowAppIcon = findViewById(R.id.row_app_icon);
        mIvProjectLogo = findViewById(R.id.iv_project_logo);
        mEtAppName = findViewById(R.id.et_app_name);
        mEtPackageName = findViewById(R.id.et_package_name);
        mEtMinSdk = findViewById(R.id.et_min_sdk);
        mEtTargetSdk = findViewById(R.id.et_target_sdk);
        mEtSaveLocation = findViewById(R.id.et_save_location);
        mEtLanguage = findViewById(R.id.et_language);
        mBtnPreviousToTemplates = findViewById(R.id.btn_previous_to_templates);
        mBtnDoneCreateProject = findViewById(R.id.btn_done_create_project);

        // Preload defaults from preferences
        mEtMinSdk.setText(String.valueOf(mPrefs.getDefaultMinSdk()));
        mEtTargetSdk.setText(String.valueOf(mPrefs.getDefaultTargetSdk()));
        mEtSaveLocation.setText(ProjectStorage.getProjectsRoot().getAbsolutePath() + "/");
        mEtLanguage.setText("JAVA");
    }

    private void setupTemplateSelection() {
        View.OnClickListener clickListener = v -> {
            mCardNoActivity.setBackgroundResource(R.drawable.bg_card_template_unselected);
            mCardEmptyActivity.setBackgroundResource(R.drawable.bg_card_template_unselected);
            mCardBasicActivity.setBackgroundResource(R.drawable.bg_card_template_unselected);
            mCardComposeActivity.setBackgroundResource(R.drawable.bg_card_template_unselected);

            int id = v.getId();
            if (id == R.id.card_template_no_activity) {
                mSelectedTemplate = "No Activity";
                mCardNoActivity.setBackgroundResource(R.drawable.bg_card_template_selected);
            } else if (id == R.id.card_template_empty_activity) {
                mSelectedTemplate = "Empty Activity";
                mCardEmptyActivity.setBackgroundResource(R.drawable.bg_card_template_selected);
            } else if (id == R.id.card_template_basic_activity) {
                mSelectedTemplate = "Basic Activity";
                mCardBasicActivity.setBackgroundResource(R.drawable.bg_card_template_selected);
            } else if (id == R.id.card_template_compose_activity) {
                mSelectedTemplate = "Compose Activity";
                mCardComposeActivity.setBackgroundResource(R.drawable.bg_card_template_selected);
            }
            mBtnNextToConfigure.setEnabled(true);
        };

        mCardNoActivity.setOnClickListener(clickListener);
        mCardEmptyActivity.setOnClickListener(clickListener);
        mCardBasicActivity.setOnClickListener(clickListener);
        mCardComposeActivity.setOnClickListener(clickListener);
    }

    private void setupStepNavigation() {
        mBtnExitTemplates.setOnClickListener(v -> finish());

        mBtnNextToConfigure.setOnClickListener(v -> {
            mLayoutStepTemplates.setVisibility(View.GONE);
            mLayoutStepConfigure.setVisibility(View.VISIBLE);
            validateInputs();
        });

        mBtnPreviousToTemplates.setOnClickListener(v -> {
            mLayoutStepConfigure.setVisibility(View.GONE);
            mLayoutStepTemplates.setVisibility(View.VISIBLE);
        });

        mRowAppIcon.setOnClickListener(v -> mGetIconLauncher.launch("image/*"));

        mBtnDoneCreateProject.setOnClickListener(v -> handleCreateProject());
    }

    private void setupInputValidation() {
        mEtPackageName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mUserEditedPackageName = true;
            }
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mEtAppName.hasFocus() && !mUserEditedPackageName) {
                    String clean = mEtAppName.getText().toString().trim().toLowerCase().replaceAll("[^a-z0-9]", "");
                    if (!clean.isEmpty()) {
                        mEtPackageName.setText("com.example." + clean);
                    }
                }
                validateInputs();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        mEtAppName.addTextChangedListener(watcher);
        mEtPackageName.addTextChangedListener(watcher);
        mEtMinSdk.addTextChangedListener(watcher);
        mEtTargetSdk.addTextChangedListener(watcher);
    }

    private boolean validateInputs() {
        String appName = mEtAppName.getText().toString().trim();
        String pkg = mEtPackageName.getText().toString().trim();
        String minStr = mEtMinSdk.getText().toString().trim();
        String targetStr = mEtTargetSdk.getText().toString().trim();

        boolean valid = true;

        if (appName.isEmpty()) {
            valid = false;
        }

        if (pkg.isEmpty() || !pkg.matches("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")) {
            valid = false;
        }

        try {
            int minSdk = Integer.parseInt(minStr);
            int targetSdk = Integer.parseInt(targetStr);
            if (minSdk < 14 || targetSdk < minSdk) {
                valid = false;
            }
        } catch (Exception e) {
            valid = false;
        }

        mBtnDoneCreateProject.setEnabled(valid);
        return valid;
    }

    private void handleCreateProject() {
        if (!validateInputs()) return;

        String appName = mEtAppName.getText().toString().trim();
        String pkg = mEtPackageName.getText().toString().trim();
        int minSdk = Integer.parseInt(mEtMinSdk.getText().toString().trim());
        int targetSdk = Integer.parseInt(mEtTargetSdk.getText().toString().trim());

        try {
            Project project = ProjectStorage.createProject(appName, pkg, mSelectedTemplate, minSdk, targetSdk, mSelectedIconBitmap);
            Toast.makeText(this, "Project created successfully!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(CreateProjectActivity.this, EditorActivity.class);
            intent.putExtra("project_path", project.getRootDirectory().getAbsolutePath());
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error creating project: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (mLayoutStepConfigure.getVisibility() == View.VISIBLE) {
            mLayoutStepConfigure.setVisibility(View.GONE);
            mLayoutStepTemplates.setVisibility(View.VISIBLE);
        } else {
            super.onBackPressed();
        }
    }
}
