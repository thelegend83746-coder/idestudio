package com.ide.studio;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.ide.studio.adapter.ProjectAdapter;
import com.ide.studio.core.FileUtils;
import com.ide.studio.core.PreferencesManager;
import com.ide.studio.core.ProjectStorage;
import com.ide.studio.model.Project;
import java.io.File;
import java.util.List;

/**
 * Main launcher activity: displays the BUILD STUDIO projects dashboard,
 * provides animated "Create Project" action, project options (Open, Export, Delete),
 * and access to IDE Settings.
 */
public class MainActivity extends AppCompatActivity implements ProjectAdapter.OnProjectClickListener {

    private static final int PERMISSION_REQ_CODE = 1001;

    private View mRootView;
    private MaterialToolbar mToolbar;
    private RecyclerView mRecyclerProjects;
    private LinearLayout mLayoutEmptyState;
    private FloatingActionButton mFabNewProject;
    private LinearLayout mBtnCreateProjectAction;
    private boolean mIsFabActionOpen = false;

    private ProjectAdapter mAdapter;
    private ProjectStorage mProjectStorage;
    private PreferencesManager mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefs = new PreferencesManager(this);
        mProjectStorage = new ProjectStorage(this);

        initViews();
        setupToolbar();
        setupFabInteraction();
        checkAndRequestStoragePermissions();
    }

    private void initViews() {
        mRootView = findViewById(R.id.coordinator_main);
        mToolbar = findViewById(R.id.toolbar);
        mRecyclerProjects = findViewById(R.id.recycler_projects);
        mLayoutEmptyState = findViewById(R.id.layout_empty_state);
        mFabNewProject = findViewById(R.id.fab_new_project);
        mBtnCreateProjectAction = findViewById(R.id.btn_create_project_action);

        mRecyclerProjects.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new ProjectAdapter(this, this);
        mRecyclerProjects.setAdapter(mAdapter);
    }

    private void setupToolbar() {
        mToolbar.inflateMenu(R.menu.menu_main);
        mToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            } else if (item.getItemId() == R.id.action_about) {
                startActivity(new Intent(MainActivity.this, AboutPageActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupFabInteraction() {
        mFabNewProject.setOnClickListener(v -> toggleCreateAction());

        mBtnCreateProjectAction.setOnClickListener(v -> {
            hideCreateAction();
            Intent intent = new Intent(MainActivity.this, CreateProjectActivity.class);
            startActivity(intent);
        });

        // Tap on coordinator background closes the action if open
        mRootView.setOnClickListener(v -> {
            if (mIsFabActionOpen) {
                hideCreateAction();
            }
        });
    }

    private void toggleCreateAction() {
        if (mIsFabActionOpen) {
            hideCreateAction();
        } else {
            showCreateAction();
        }
    }

    private void showCreateAction() {
        mIsFabActionOpen = true;
        mBtnCreateProjectAction.setVisibility(View.VISIBLE);
        mBtnCreateProjectAction.setAlpha(0f);
        mBtnCreateProjectAction.setScaleX(0.7f);
        mBtnCreateProjectAction.setScaleY(0.7f);
        mBtnCreateProjectAction.setTranslationY(20f * getResources().getDisplayMetrics().density);

        mBtnCreateProjectAction.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(180)
                .setListener(null)
                .start();

        mFabNewProject.animate()
                .rotation(45f)
                .setDuration(180)
                .start();
    }

    private void hideCreateAction() {
        mIsFabActionOpen = false;
        mBtnCreateProjectAction.animate()
                .alpha(0f)
                .scaleX(0.7f)
                .scaleY(0.7f)
                .translationY(20f * getResources().getDisplayMetrics().density)
                .setDuration(150)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mBtnCreateProjectAction.setVisibility(View.GONE);
                    }
                })
                .start();

        mFabNewProject.animate()
                .rotation(0f)
                .setDuration(150)
                .start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mIsFabActionOpen) {
            hideCreateAction();
        }
        loadProjects();
    }

    private void checkAndRequestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQ_CODE);
            }
        }
    }

    private void loadProjects() {
        List<Project> projects = mProjectStorage.getAllProjects();
        mAdapter.setProjects(projects);

        if (projects.isEmpty()) {
            mLayoutEmptyState.setVisibility(View.VISIBLE);
            mRecyclerProjects.setVisibility(View.GONE);
        } else {
            mLayoutEmptyState.setVisibility(View.GONE);
            mRecyclerProjects.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onProjectClick(Project project) {
        Intent intent = new Intent(MainActivity.this, EditorActivity.class);
        intent.putExtra("project_path", project.getRootDirectory().getAbsolutePath());
        startActivity(intent);
    }

    @Override
    public void onProjectLongClick(Project project) {
        showProjectActionsDialog(project);
    }

    private void showProjectActionsDialog(Project project) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_project_actions, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.dialog_title);
        TextView btnOpen = dialogView.findViewById(R.id.btn_action_open);
        TextView btnExport = dialogView.findViewById(R.id.btn_action_export);
        TextView btnDelete = dialogView.findViewById(R.id.btn_action_delete);

        tvTitle.setText(project.getName());

        btnOpen.setOnClickListener(v -> {
            dialog.dismiss();
            onProjectClick(project);
        });

        btnExport.setOnClickListener(v -> {
            dialog.dismiss();
            exportProject(project);
        });

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            handleDeleteProject(project);
        });

        dialog.show();
    }

    private void exportProject(Project project) {
        File exportDir = ProjectStorage.getExportsRoot();
        exportDir.mkdirs();
        File zipFile = new File(exportDir, project.getName() + ".zip");
        boolean success = FileUtils.zipDirectory(project.getRootDirectory(), zipFile);
        if (success) {
            Snackbar.make(mRootView, "Exported to: " + zipFile.getName(), Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(mRootView, "Export failed", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void handleDeleteProject(Project project) {
        if (mPrefs.isConfirmBeforeDelete()) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Project?")
                    .setMessage("Are you sure you want to delete '" + project.getName() + "'? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        executeDelete(project);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            executeDelete(project);
        }
    }

    private void executeDelete(Project project) {
        boolean deleted = mProjectStorage.deleteProject(project);
        if (deleted) {
            loadProjects();
            Snackbar.make(mRootView, "Project deleted", Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(mRootView, "Failed to delete project", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (mIsFabActionOpen) {
            hideCreateAction();
        } else {
            super.onBackPressed();
        }
    }
}
