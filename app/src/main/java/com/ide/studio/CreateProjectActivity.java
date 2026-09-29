package com.ide.studio;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.ide.studio.core.ProjectStorage;
import com.ide.studio.model.Project;

/**
 * Wizard for creating a new project with template selection and custom icon support.
 */
public class CreateProjectActivity extends AppCompatActivity {

    private ImageView mBtnBack;
    private ImageView mIvProjectIcon;
    private Button mBtnChooseIcon;
    private EditText mEtAppName;
    private EditText mEtPackageName;
    private LinearLayout mCardEmpty;
    private LinearLayout mCardNav;
    private LinearLayout mCardSurface;
    private LinearLayout mCardCanvas;
    private Button mBtnCreate;

    private String mSelectedTemplate = "Empty Activity";
    private Bitmap mSelectedIconBitmap = null;
    private boolean mUserEditedPackageName = false;

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
                        mIvProjectIcon.setImageBitmap(mSelectedIconBitmap);
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to load icon", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_project);

        mBtnBack = findViewById(R.id.btn_back);
        mIvProjectIcon = findViewById(R.id.iv_project_icon);
        mBtnChooseIcon = findViewById(R.id.btn_choose_icon);
        mEtAppName = findViewById(R.id.et_app_name);
        mEtPackageName = findViewById(R.id.et_package_name);
        mCardEmpty = findViewById(R.id.card_template_empty);
        mCardNav = findViewById(R.id.card_template_navigation);
        mCardSurface = findViewById(R.id.card_template_surface_game);
        mCardCanvas = findViewById(R.id.card_template_canvas_game);
        mBtnCreate = findViewById(R.id.btn_create_project);

        mBtnBack.setOnClickListener(v -> finish());
        mBtnChooseIcon.setOnClickListener(v -> mGetIconLauncher.launch("image/*"));

        setupTemplateSelectors();
        setupPackageAutoSuggest();

        mBtnCreate.setOnClickListener(v -> handleCreateProject());
    }

    private void setupPackageAutoSuggest() {
        mEtPackageName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mUserEditedPackageName = true;
            }
        });

        mEtAppName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!mUserEditedPackageName) {
                    String clean = s.toString().trim().toLowerCase().replaceAll("[^a-z0-9]", "");
                    if (!clean.isEmpty()) {
                        mEtPackageName.setText("com.example." + clean);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupTemplateSelectors() {
        View.OnClickListener listener = v -> {
            resetTemplateCardStyles();
            int id = v.getId();
            if (id == R.id.card_template_empty) {
                mSelectedTemplate = "Empty Activity";
                mCardEmpty.setBackgroundResource(R.drawable.bg_chat_card);
            } else if (id == R.id.card_template_navigation) {
                mSelectedTemplate = "Bottom Nav";
                mCardNav.setBackgroundResource(R.drawable.bg_chat_card);
            } else if (id == R.id.card_template_surface_game) {
                mSelectedTemplate = "SurfaceView Game";
                mCardSurface.setBackgroundResource(R.drawable.bg_chat_card);
            } else if (id == R.id.card_template_canvas_game) {
                mSelectedTemplate = "Canvas 2D Game";
                mCardCanvas.setBackgroundResource(R.drawable.bg_chat_card);
            }
        };

        mCardEmpty.setOnClickListener(listener);
        mCardNav.setOnClickListener(listener);
        mCardSurface.setOnClickListener(listener);
        mCardCanvas.setOnClickListener(listener);

        // Default empty selected
        resetTemplateCardStyles();
        mCardEmpty.setBackgroundResource(R.drawable.bg_chat_card);
    }

    private void resetTemplateCardStyles() {
        mCardEmpty.setBackgroundResource(R.drawable.bg_card_rounded);
        mCardNav.setBackgroundResource(R.drawable.bg_card_rounded);
        mCardSurface.setBackgroundResource(R.drawable.bg_card_rounded);
        mCardCanvas.setBackgroundResource(R.drawable.bg_card_rounded);
    }

    private void handleCreateProject() {
        String appName = mEtAppName.getText().toString().trim();
        String pkg = mEtPackageName.getText().toString().trim();

        if (appName.isEmpty()) {
            mEtAppName.setError("Please enter an application name");
            return;
        }

        if (pkg.isEmpty() || !pkg.contains(".")) {
            mEtPackageName.setError("Please enter a valid package name (e.g. com.example.app)");
            return;
        }

        try {
            Project project = ProjectStorage.createProject(appName, pkg, mSelectedTemplate, 21, 33, mSelectedIconBitmap);
            Toast.makeText(this, "Project created successfully!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(CreateProjectActivity.this, EditorActivity.class);
            intent.putExtra("project_path", project.getRootDirectory().getAbsolutePath());
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error creating project: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
