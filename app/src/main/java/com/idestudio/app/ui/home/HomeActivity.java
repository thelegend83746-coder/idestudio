package com.idestudio.app.ui.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.idestudio.app.R;
import com.idestudio.app.domain.project.LocalProjectStore;
import com.idestudio.app.domain.project.ProjectExporter;
import com.idestudio.app.domain.project.ProjectMeta;
import com.idestudio.app.ui.create.ConfigureProjectActivity;
import com.idestudio.app.ui.create.EditProjectActivity;
import com.idestudio.app.ui.editor.EditorActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Home screen displaying projects from /storage/emulated/0/idestudio.
 * Supports quick search, opening editor, editing project details,
 * and exporting .zip backups.
 */
public class HomeActivity extends AppCompatActivity implements ProjectListAdapter.OnProjectClickListener {

    private static final int RC_EDIT_PROJECT = 2001;

    private RecyclerView rvProjects;
    private ProjectListAdapter adapter;
    private View emptyStateLayout;
    private EditText etSearch;
    private FloatingActionButton fabNewProject;

    private List<ProjectMeta> allProjects = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        loadProjects();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
    }

    private void initViews() {
        rvProjects = findViewById(R.id.rv_projects);
        emptyStateLayout = findViewById(R.id.layout_empty_state);
        etSearch = findViewById(R.id.et_search_projects);
        fabNewProject = findViewById(R.id.fab_new_project);

        adapter = new ProjectListAdapter(this);
        rvProjects.setLayoutManager(new LinearLayoutManager(this));
        rvProjects.setAdapter(adapter);

        fabNewProject.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ConfigureProjectActivity.class);
            startActivity(intent);
        });

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterProjects(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
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

    private void filterProjects(String query) {
        if (query == null || query.trim().isEmpty()) {
            adapter.setProjects(allProjects);
            return;
        }

        String lower = query.toLowerCase().trim();
        List<ProjectMeta> filtered = new ArrayList<>();
        for (ProjectMeta p : allProjects) {
            if (p.getName().toLowerCase().contains(lower) || p.getPackageName().toLowerCase().contains(lower)) {
                filtered.add(p);
            }
        }
        adapter.setProjects(filtered);
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
