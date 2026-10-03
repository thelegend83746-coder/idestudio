package com.idestudio.app.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.idestudio.app.R;
import com.idestudio.app.data.models.ProjectMeta;
import com.idestudio.app.domain.project.LocalProjectStore;
import com.idestudio.app.domain.project.ProjectExporter;
import com.idestudio.app.ui.create.ChooseTemplateActivity;
import com.idestudio.app.ui.create.ConfigureProjectActivity;
import com.idestudio.app.ui.create.EditProjectActivity;
import com.idestudio.app.ui.editor.EditorActivity;
import com.idestudio.app.ui.settings.SettingsActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Home screen displaying projects from /storage/emulated/0/idestudio.
 * Supports quick search, opening editor, editing project details,
 * and exporting .zip backups.
 */
public class HomeActivity extends AppCompatActivity implements ProjectAdapter.OnProjectInteractionListener {

    private static final int RC_EDIT_PROJECT = 2001;
    private static final int RC_STORAGE_PERM = 1001;

    private RecyclerView rvProjects;
    private ProjectAdapter adapter;
    private View emptyStateLayout;
    private FloatingActionButton fabNewProject;

    private List<ProjectMeta> allProjects = new ArrayList<>();
    private boolean permissionRequested = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        loadProjects();
        checkStoragePermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
    }

    private void checkStoragePermission() {
        if (permissionRequested) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                permissionRequested = true;
                new AlertDialog.Builder(this)
                        .setTitle("Storage Permission Required")
                        .setMessage("IDE Studio needs All Files Access to save and load projects in /storage/emulated/0/idestudio so you can easily access them.\n\nPlease allow access in the next screen.")
                        .setPositiveButton("Grant Access", (d, w) -> {
                            try {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            } catch (Exception e) {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("Later", null)
                        .show();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionRequested = true;
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, RC_STORAGE_PERM);
            }
        }
    }

    private void initViews() {
        rvProjects = findViewById(R.id.recycler_projects);
        emptyStateLayout = findViewById(R.id.layout_empty_state);
        fabNewProject = findViewById(R.id.fab_add);

        adapter = new ProjectAdapter(this);
        rvProjects.setLayoutManager(new LinearLayoutManager(this));
        rvProjects.setAdapter(adapter);

        View.OnClickListener openChooseTemplate = v -> {
            Intent intent = new Intent(HomeActivity.this, ChooseTemplateActivity.class);
            startActivity(intent);
        };

        fabNewProject.setOnClickListener(openChooseTemplate);

        View btnCreateOption = findViewById(R.id.container_create_project_option);
        if (btnCreateOption != null) {
            btnCreateOption.setOnClickListener(openChooseTemplate);
        }

        if (emptyStateLayout != null) {
            emptyStateLayout.setOnClickListener(openChooseTemplate);
        }

        View btnSettings = findViewById(R.id.btn_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        }
    }

    private void loadProjects() {
        allProjects = LocalProjectStore.getInstance().getAllProjects();
        if (allProjects.isEmpty()) {
            rvProjects.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            rvProjects.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            adapter.setProjects(allProjects);
        }
    }

    @Override
    public void onProjectClick(ProjectMeta project) {
        openProjectInEditor(project);
    }

    @Override
    public void onProjectLongClick(ProjectMeta project) {
        showProjectActionsDialog(project);
    }

    private void openProjectInEditor(ProjectMeta project) {
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra("project_id", project.getId());
        intent.putExtra("project_path", project.getProjectPath());
        intent.putExtra("project_name", project.getName());
        startActivity(intent);
    }

    private void showProjectActionsDialog(ProjectMeta project) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_project_actions, null);

        TextView tvTitle = dialogView.findViewById(R.id.dialog_project_title);
        TextView tvSubtitle = dialogView.findViewById(R.id.dialog_project_subtitle);
        TextView btnOpen = dialogView.findViewById(R.id.action_dialog_open);
        TextView btnEdit = dialogView.findViewById(R.id.action_dialog_edit);
        TextView btnExport = dialogView.findViewById(R.id.action_dialog_export);
        TextView btnDelete = dialogView.findViewById(R.id.action_dialog_delete);

        tvTitle.setText(project.getName());
        tvSubtitle.setText(project.getPackageName());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnOpen.setOnClickListener(v -> {
            dialog.dismiss();
            openProjectInEditor(project);
        });

        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(HomeActivity.this, EditProjectActivity.class);
            intent.putExtra(EditProjectActivity.EXTRA_PROJECT_ID, project.getId());
            startActivityForResult(intent, RC_EDIT_PROJECT);
        });

        btnExport.setOnClickListener(v -> {
            dialog.dismiss();
            exportProjectBackup(project);
        });

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            confirmDeleteProject(project);
        });

        dialog.show();
    }

    private void exportProjectBackup(ProjectMeta project) {
        Toast.makeText(this, "Exporting " + project.getName() + " backup...", Toast.LENGTH_SHORT).show();

        ProjectExporter.exportProjectAsync(project, new ProjectExporter.ExportCallback() {
            @Override
            public void onSuccess(File zipFile) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    new AlertDialog.Builder(HomeActivity.this)
                            .setTitle("Backup Created Successfully")
                            .setMessage("Project exported to:\n\n" + zipFile.getAbsolutePath() + "\n\nSize: " + (zipFile.length() / 1024) + " KB")
                            .setPositiveButton("Share .zip", (d, w) -> shareZipFile(zipFile))
                            .setNegativeButton("Done", null)
                            .show();
                });
            }

            @Override
            public void onError(String errorMessage) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(HomeActivity.this, "Export failed: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void shareZipFile(File zipFile) {
        try {
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", zipFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/zip");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Project Backup"));
        } catch (Exception e) {
            Toast.makeText(this, "File saved to: " + zipFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDeleteProject(ProjectMeta project) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Project")
                .setMessage("Are you sure you want to permanently delete '" + project.getName() + "' and all its files?")
                .setPositiveButton("Delete", (d, w) -> {
                    LocalProjectStore.getInstance().deleteProject(project.getId());
                    loadProjects();
                    Toast.makeText(HomeActivity.this, "Project deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_EDIT_PROJECT && resultCode == RESULT_OK) {
            loadProjects();
        }
    }
}
