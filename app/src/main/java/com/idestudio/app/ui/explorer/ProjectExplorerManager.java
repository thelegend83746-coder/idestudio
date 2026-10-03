package com.idestudio.app.ui.explorer;

import android.content.Context;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.idestudio.app.R;
import com.idestudio.app.data.models.FileNode;
import com.idestudio.app.domain.filesystem.FileTreeManager;
import com.idestudio.app.domain.filesystem.SafeFileOperations;

import java.io.File;

public class ProjectExplorerManager implements FileTreeAdapter.OnFileTreeInteractionListener {

    public interface FileSelectionCallback {
        void onFileSelected(File file);
    }

    public interface DrawerCloseCallback {
        void onCloseRequested();
    }

    private final Context context;
    private final File projectRoot;
    private final View drawerView;
    private final FileSelectionCallback fileSelectionCallback;
    private final DrawerCloseCallback drawerCloseCallback;

    private FileTreeManager treeManager;
    private FileTreeAdapter treeAdapter;

    public ProjectExplorerManager(Context context, File projectRoot, View drawerView,
                                  FileSelectionCallback fileSelectionCallback,
                                  DrawerCloseCallback drawerCloseCallback) {
        this.context = context;
        this.projectRoot = projectRoot;
        this.drawerView = drawerView;
        this.fileSelectionCallback = fileSelectionCallback;
        this.drawerCloseCallback = drawerCloseCallback;

        initViews();
        setupSwipeToClose();
    }

    private void initViews() {
        TextView tvProjectName = drawerView.findViewById(R.id.tv_drawer_project_name);
        tvProjectName.setText(projectRoot.getName());

        RecyclerView recycler = drawerView.findViewById(R.id.recycler_file_tree);
        recycler.setLayoutManager(new LinearLayoutManager(context));

        treeAdapter = new FileTreeAdapter(this);
        recycler.setAdapter(treeAdapter);

        treeManager = new FileTreeManager(projectRoot);
        refreshTree();
    }

    public void refreshTree() {
        treeManager.reload();
        treeAdapter.setNodes(treeManager.getVisibleNodes());
    }

    private void setupSwipeToClose() {
        GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 80;
            private static final int SWIPE_VELOCITY_THRESHOLD = 80;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e2 != null) {
                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();
                    // Right to Left swipe: diffX is negative
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (diffX < -SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (drawerCloseCallback != null) {
                                drawerCloseCallback.onCloseRequested();
                            }
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        drawerView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    @Override
    public void onFileClicked(File file) {
        if (fileSelectionCallback != null) {
            fileSelectionCallback.onFileSelected(file);
        }
        if (drawerCloseCallback != null) {
            drawerCloseCallback.onCloseRequested();
        }
    }

    @Override
    public void onFolderClicked(FileNode folderNode) {
        folderNode.setExpanded(!folderNode.isExpanded());
        treeAdapter.setNodes(treeManager.getVisibleNodes());
    }

    @Override
    public void onFolderLongClicked(File folder) {
        showFolderActionsDialog(folder);
    }

    @Override
    public void onFileLongClicked(File file) {
        showFileActionsDialog(file);
    }

    private void showFolderActionsDialog(File folder) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_folder_actions, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_folder_actions_title);
        TextView btnRename = dialogView.findViewById(R.id.action_folder_rename);
        TextView btnCreateFile = dialogView.findViewById(R.id.action_folder_create_file);
        TextView btnCreateFolder = dialogView.findViewById(R.id.action_folder_create_folder);
        TextView btnDelete = dialogView.findViewById(R.id.action_folder_delete);

        tvTitle.setText("Folder: " + folder.getName());

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .create();

        btnRename.setOnClickListener(v -> {
            dialog.dismiss();
            showRenameDialog(folder, true);
        });

        btnCreateFile.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateFileDialog(folder);
        });

        btnCreateFolder.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateFolderDialog(folder);
        });

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteConfirmDialog(folder, true);
        });

        dialog.show();
    }

    private void showFileActionsDialog(File file) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_file_actions, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_file_actions_title);
        TextView btnRename = dialogView.findViewById(R.id.action_file_rename);
        TextView btnDelete = dialogView.findViewById(R.id.action_file_delete);

        tvTitle.setText("File: " + file.getName());

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .create();

        btnRename.setOnClickListener(v -> {
            dialog.dismiss();
            showRenameDialog(file, false);
        });

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteConfirmDialog(file, false);
        });

        dialog.show();
    }

    private void showRenameDialog(File target, boolean isFolder) {
        EditText input = new EditText(context);
        input.setText(target.getName());
        input.setSelectAllOnFocus(true);

        new MaterialAlertDialogBuilder(context)
                .setTitle(isFolder ? "Rename Folder" : "Rename File")
                .setView(input)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        try {
                            SafeFileOperations.rename(projectRoot, target, newName);
                            refreshTree();
                            Toast.makeText(context, "Renamed successfully", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .show();
    }

    private void showCreateFileDialog(File parentFolder) {
        EditText input = new EditText(context);
        input.setHint("e.g. MyClass.java");

        new MaterialAlertDialogBuilder(context)
                .setTitle("Create File")
                .setView(input)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_create, (dialog, which) -> {
                    String fileName = input.getText().toString().trim();
                    if (!fileName.isEmpty()) {
                        try {
                            File newFile = SafeFileOperations.createFile(projectRoot, parentFolder, fileName);
                            refreshTree();
                            onFileClicked(newFile);
                            Toast.makeText(context, "File created", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .show();
    }

    private void showCreateFolderDialog(File parentFolder) {
        EditText input = new EditText(context);
        input.setHint("e.g. models");

        new MaterialAlertDialogBuilder(context)
                .setTitle("Create Folder")
                .setView(input)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_create, (dialog, which) -> {
                    String folderName = input.getText().toString().trim();
                    if (!folderName.isEmpty()) {
                        try {
                            SafeFileOperations.createFolder(projectRoot, parentFolder, folderName);
                            refreshTree();
                            Toast.makeText(context, "Folder created", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .show();
    }

    private void showDeleteConfirmDialog(File target, boolean isFolder) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Delete " + (isFolder ? "Folder" : "File"))
                .setMessage("Are you sure you want to delete '" + target.getName() + "'? This cannot be undone.")
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    try {
                        SafeFileOperations.delete(projectRoot, target);
                        refreshTree();
                        Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }
}
