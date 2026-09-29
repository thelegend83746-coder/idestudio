package com.ide.studio;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
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
import com.ide.studio.adapter.ProjectAdapter;
import com.ide.studio.core.FileUtils;
import com.ide.studio.core.ProjectStorage;
import com.ide.studio.model.Project;
import java.io.File;
import java.util.List;

/**
 * Main launcher activity: displays the list of user projects,
 * allows creating new projects, and navigating to settings or the IDE editor.
 */
public class MainActivity extends AppCompatActivity implements ProjectAdapter.OnProjectClickListener {

    private static final int PERMISSION_REQ_CODE = 1001;

    private MaterialToolbar mToolbar;
    private RecyclerView mRecyclerProjects;
    private TextView mTvEmpty;
    private FloatingActionButton mFabNewProject;
    private ProjectAdapter mAdapter;
    private ProjectStorage mProjectStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProjectStorage = new ProjectStorage(this);

        mToolbar = findViewById(R.id.toolbar);
        mRecyclerProjects = findViewById(R.id.recycler_projects);
        mTvEmpty = findViewById(R.id.tv_empty);
        mFabNewProject = findViewById(R.id.fab_new_project);

        mToolbar.inflateMenu(R.menu.menu_main);
        mToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            }
            return false;
        });

        mRecyclerProjects.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new ProjectAdapter(this, this);
        mRecyclerProjects.setAdapter(mAdapter);

        mFabNewProject.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateProjectActivity.class);
            startActivity(intent);
        });

        checkAndRequestStoragePermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
            mTvEmpty.setVisibility(View.VISIBLE);
            mRecyclerProjects.setVisibility(View.GONE);
        } else {
            mTvEmpty.setVisibility(View.GONE);
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
            confirmDeleteProject(project);
        });

        dialog.show();
    }

    private void exportProject(Project project) {
        File exportDir = new File(Environment.getExternalStorageDirectory(), "test-folder/exports");
        exportDir.mkdirs();
        File zipFile = new File(exportDir, project.getName() + ".zip");
        boolean success = FileUtils.zipDirectory(project.getRootDirectory(), zipFile);
        if (success) {
            Toast.makeText(this, "Exported to: " + zipFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteProject(Project project) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Project")
                .setMessage("Are you sure you want to delete '" + project.getName() + "'? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    mProjectStorage.deleteProject(project);
                    loadProjects();
                    Toast.makeText(this, "Project deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
