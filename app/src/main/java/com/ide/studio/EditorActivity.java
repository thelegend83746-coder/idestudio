package com.ide.studio;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ide.studio.adapter.FileTreeAdapter;
import com.ide.studio.compiler.ApkBuildPipeline;
import com.ide.studio.core.FileUtils;
import com.ide.studio.core.PreferencesManager;
import com.ide.studio.core.ProjectStorage;
import com.ide.studio.model.Project;
import com.ide.studio.view.BuildLogDialog;
import com.ide.studio.view.SmoothCodeEditor;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Main IDE Code Editor activity.
 * Integrates smooth kinetic overscroll editor, navigation drawer file explorer,
 * multiple file tabs, fast undo/redo, auto-save, and APK compilation toolchain.
 */
public class EditorActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    private TextView mTvProjectTitle;
    private TextView mBtnUndo;
    private TextView mBtnRedo;
    private ImageView mBtnFolder;
    private TextView mBtnRun;
    private ImageView mBtnMore;
    private LinearLayout mLayoutTabs;
    private SmoothCodeEditor mEditor;
    private LinearLayout mPillSaveFile;
    private RecyclerView mRecyclerFileTree;
    private FileTreeAdapter mFileTreeAdapter;

    private Project mCurrentProject;
    private File mCurrentOpenFile;
    private final List<File> mOpenFiles = new ArrayList<>();
    private boolean mHasUnsavedChanges = false;
    private PreferencesManager mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        mPrefs = new PreferencesManager(this);

        initViews();
        resolveProject();
        setupEditor();
        setupFileTree();
        setupToolbarActions();

        openInitialFile();
    }

    private void initViews() {
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mTvProjectTitle = findViewById(R.id.tv_project_title);
        mBtnUndo = findViewById(R.id.btn_undo);
        mBtnRedo = findViewById(R.id.btn_redo);
        mBtnFolder = findViewById(R.id.btn_folder);
        mBtnRun = findViewById(R.id.btn_run);
        mBtnMore = findViewById(R.id.btn_more);
        mLayoutTabs = findViewById(R.id.layout_tabs);
        mEditor = findViewById(R.id.code_editor);
        mPillSaveFile = findViewById(R.id.pill_save_file);
        mRecyclerFileTree = findViewById(R.id.recycler_file_tree);

        findViewById(R.id.btn_drawer).setOnClickListener(v -> toggleDrawer());
        mBtnFolder.setOnClickListener(v -> toggleDrawer());
    }

    private void resolveProject() {
        String path = getIntent().getStringExtra("project_path");
        if (path != null) {
            File root = new File(path);
            if (root.exists()) {
                mCurrentProject = new Project(root.getName(), "com.example", root.getAbsolutePath());
            }
        }

        if (mCurrentProject == null) {
            List<Project> list = ProjectStorage.loadProjects();
            if (!list.isEmpty()) {
                mCurrentProject = list.get(0);
            } else {
                Toast.makeText(this, "No project loaded", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        mTvProjectTitle.setText(mCurrentProject.getName());
    }

    private void setupEditor() {
        mEditor.setLineNumbersVisible(mPrefs.showLineNumbers());
        mEditor.setTextSize(mPrefs.getEditorFontSize());

        mEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mCurrentOpenFile != null && !mHasUnsavedChanges) {
                    mHasUnsavedChanges = true;
                    mPillSaveFile.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        mPillSaveFile.setOnClickListener(v -> saveCurrentFile());
    }

    private void setupFileTree() {
        mRecyclerFileTree.setLayoutManager(new LinearLayoutManager(this));
        mFileTreeAdapter = new FileTreeAdapter(this, file -> {
            openFile(file);
            mDrawerLayout.closeDrawer(GravityCompat.START);
        });
        mRecyclerFileTree.setAdapter(mFileTreeAdapter);
        mFileTreeAdapter.setRootDirectory(mCurrentProject.getRootDirectory());
    }

    private void setupToolbarActions() {
        mBtnUndo.setOnClickListener(v -> {
            if (mEditor.canUndo()) {
                mEditor.undo();
            } else {
                Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show();
            }
        });

        mBtnRedo.setOnClickListener(v -> {
            if (mEditor.canRedo()) {
                mEditor.redo();
            } else {
                Toast.makeText(this, "Nothing to redo", Toast.LENGTH_SHORT).show();
            }
        });

        mBtnRun.setOnClickListener(v -> startBuildPipeline());

        mBtnMore.setOnClickListener(v -> showMoreMenu(v));
    }

    private void toggleDrawer() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }

    private void showMoreMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Build AI ✨");
        popup.getMenu().add(0, 2, 1, "Settings");
        popup.getMenu().add(0, 3, 2, "Project Info");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                Intent aiIntent = new Intent(EditorActivity.this, BuildAiActivity.class);
                aiIntent.putExtra("project_path", mCurrentProject.getRootDirectory().getAbsolutePath());
                startActivity(aiIntent);
                return true;
            } else if (item.getItemId() == 2) {
                startActivity(new Intent(EditorActivity.this, SettingsActivity.class));
                return true;
            } else if (item.getItemId() == 3) {
                showProjectInfoDialog();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void showProjectInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle(mCurrentProject.getName())
                .setMessage("Path: " + mCurrentProject.getRootDirectory().getAbsolutePath() +
                        "\nPackage: " + mCurrentProject.getPackageName() +
                        "\nTarget SDK: " + mCurrentProject.getTargetSdk() +
                        "\nTemplate: " + mCurrentProject.getTemplateType())
                .setPositiveButton("Close", null)
                .show();
    }

    private void openInitialFile() {
        File srcDir = new File(mCurrentProject.getRootDirectory(), "app/src/main/java");
        File candidate = findFirstJavaFile(srcDir);
        if (candidate == null) {
            candidate = new File(mCurrentProject.getRootDirectory(), "app/src/main/AndroidManifest.xml");
        }
        if (candidate.exists()) {
            openFile(candidate);
        }
    }

    private File findFirstJavaFile(File dir) {
        if (!dir.exists() || !dir.isDirectory()) return null;
        File[] children = dir.listFiles();
        if (children == null) return null;
        for (File f : children) {
            if (f.isFile() && f.getName().endsWith(".java")) {
                return f;
            }
            if (f.isDirectory()) {
                File found = findFirstJavaFile(f);
                if (found != null) return found;
            }
        }
        return null;
    }

    public void openFile(File file) {
        if (file == null || !file.exists() || file.isDirectory()) return;

        // Auto-save previous file if modified
        if (mHasUnsavedChanges && mCurrentOpenFile != null) {
            saveCurrentFile();
        }

        mCurrentOpenFile = file;
        if (!mOpenFiles.contains(file)) {
            mOpenFiles.add(file);
        }

        refreshTabs();

        try {
            String content = FileUtils.readFile(file);
            mEditor.setText(content);
            mHasUnsavedChanges = false;
            mPillSaveFile.setVisibility(View.GONE);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to read file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshTabs() {
        mLayoutTabs.removeAllViews();
        for (File f : mOpenFiles) {
            TextView tab = new TextView(this);
            tab.setText(f.getName().toUpperCase());
            tab.setTextSize(13);
            tab.setPadding(32, 16, 32, 16);
            tab.setGravity(Gravity.CENTER);

            boolean isActive = f.equals(mCurrentOpenFile);
            tab.setTextColor(isActive ? Color.parseColor("#D32F2F") : Color.parseColor("#6B7280"));
            tab.setTypeface(null, isActive ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

            tab.setOnClickListener(v -> openFile(f));
            mLayoutTabs.addView(tab);
        }
    }

    private void saveCurrentFile() {
        if (mCurrentOpenFile != null) {
            try {
                FileUtils.writeFile(mCurrentOpenFile, mEditor.getText().toString());
                mHasUnsavedChanges = false;
                mPillSaveFile.setVisibility(View.GONE);
                Toast.makeText(this, "Saved: " + mCurrentOpenFile.getName(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Save error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startBuildPipeline() {
        // Save open edits before compiling
        if (mHasUnsavedChanges) {
            saveCurrentFile();
        }

        BuildLogDialog logDialog = new BuildLogDialog(this);
        logDialog.show();

        ApkBuildPipeline.buildProject(this, mCurrentProject, logDialog, (success, apkFile) -> {
            runOnUiThread(() -> {
                if (success && apkFile != null) {
                    Toast.makeText(this, "Build Succeeded!", Toast.LENGTH_SHORT).show();
                    ApkBuildPipeline.installApk(this, apkFile);
                } else {
                    Toast.makeText(this, "Build Failed! Check log output.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mHasUnsavedChanges) {
            saveCurrentFile();
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
