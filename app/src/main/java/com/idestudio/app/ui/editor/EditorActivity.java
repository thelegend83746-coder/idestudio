package com.idestudio.app.ui.editor;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.idestudio.app.R;
import com.idestudio.app.domain.project.ProjectExporter;
import com.idestudio.app.domain.project.ProjectMeta;
import com.idestudio.app.editor.engine.JavaSyntaxHighlighter;
import com.idestudio.app.editor.tabs.EditorTabAdapter;
import com.idestudio.app.ui.ai.AIChatActivity;
import com.idestudio.app.ui.explorer.ProjectExplorerManager;
import com.idestudio.app.ui.settings.SettingsActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pro Premium Code Editor Activity with:
 * - Two-Finger Pinch-To-Zoom (Editor & Line Numbers synchronization)
 * - Mobile Programming Symbols Bar (Tab, brackets, operators)
 * - Real-Time Syntax Highlighting (Java, XML, Gradle)
 * - Full Undo / Redo History
 * - In-File Find & Replace with navigation
 * - Synchronized Line Numbers Gutter
 * - Status Bar (Cursor line/col, total lines, zoom level, language)
 * - Multi-Tab File Manager & Project Explorer Drawer
 */
public class EditorActivity extends AppCompatActivity {

    // Views
    private DrawerLayout drawerLayout;
    private TextView tvToolbarProject;
    private TextView tvToolbarFile;
    private ImageButton btnDrawerToggle;
    private ImageButton btnActionSearch;
    private ImageButton btnActionUndo;
    private ImageButton btnActionRedo;
    private ImageButton btnActionSave;
    private ImageButton btnActionAi;
    private ImageButton btnActionOverflow;

    // Editor Area
    private ScrollView scrollVertical;
    private HorizontalScrollView scrollHorizontal;
    private TextView tvLineNumbers;
    private EditText etCodeEditor;
    private TextView tvZoomBadge;
    private TextView tvEmptyEditor;

    // Tabs
    private RecyclerView rvTabs;
    private EditorTabAdapter tabAdapter;

    // Symbol Bar
    private LinearLayout layoutSymbols;

    // Status Bar
    private TextView tvStatusCursor;
    private TextView tvStatusLines;
    private TextView tvStatusZoom;
    private TextView tvStatusLang;

    // Find & Replace
    private View layoutSearchReplace;
    private EditText etSearchQuery;
    private EditText etReplaceQuery;
    private TextView tvSearchCount;

    // Controllers
    private EditorPinchZoomController zoomController;
    private EditorProFeaturesController proController;
    private EditorUndoRedoManager undoRedoManager;
    private ProjectExplorerManager explorerManager;

    // State
    private String projectName;
    private String projectPath;
    private File activeFile;
    private final List<File> openFiles = new ArrayList<>();
    private final Map<String, String> fileCache = new HashMap<>();

    private final Handler syntaxHandler = new Handler(Looper.getMainLooper());
    private Runnable syntaxRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        projectName = getIntent().getStringExtra("project_name");
        projectPath = getIntent().getStringExtra("project_path");

        if (projectPath == null) {
            Toast.makeText(this, "Project path not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupControllers();
        setupListeners();
        setupExplorerDrawer();
        openDefaultFile();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout_editor);
        tvToolbarProject = findViewById(R.id.tv_toolbar_project_name);
        tvToolbarFile = findViewById(R.id.tv_toolbar_file_name);
        btnDrawerToggle = findViewById(R.id.btn_drawer_toggle);
        btnActionSearch = findViewById(R.id.btn_action_search);
        btnActionUndo = findViewById(R.id.btn_action_undo);
        btnActionRedo = findViewById(R.id.btn_action_redo);
        btnActionSave = findViewById(R.id.btn_action_save);
        btnActionAi = findViewById(R.id.btn_action_ai);
        btnActionOverflow = findViewById(R.id.btn_action_overflow);

        scrollVertical = findViewById(R.id.scroll_vertical_editor);
        scrollHorizontal = findViewById(R.id.scroll_horizontal_editor);
        tvLineNumbers = findViewById(R.id.tv_line_numbers);
        etCodeEditor = findViewById(R.id.et_code_editor);
        tvZoomBadge = findViewById(R.id.tv_zoom_badge);
        tvEmptyEditor = findViewById(R.id.tv_empty_editor);

        rvTabs = findViewById(R.id.recycler_editor_tabs);
        layoutSymbols = findViewById(R.id.layout_symbols_container);

        tvStatusCursor = findViewById(R.id.tv_status_cursor);
        tvStatusLines = findViewById(R.id.tv_status_lines);
        tvStatusZoom = findViewById(R.id.tv_status_zoom);
        tvStatusLang = findViewById(R.id.tv_status_lang);

        layoutSearchReplace = findViewById(R.id.layout_search_replace);
        etSearchQuery = findViewById(R.id.et_search_query);
        etReplaceQuery = findViewById(R.id.et_replace_query);
        tvSearchCount = findViewById(R.id.tv_search_count);

        tvToolbarProject.setText(projectName != null ? projectName : "IDE Studio");
        tvToolbarFile.setText("Select a file");
    }

    private void setupControllers() {
        // 1. Two-Finger Pinch-To-Zoom Controller
        zoomController = new EditorPinchZoomController(
                this,
                etCodeEditor,
                tvLineNumbers,
                tvZoomBadge,
                tvStatusZoom,
                scrollVertical,
                scrollHorizontal
        );

        // 2. Pro Features Controller (Symbols bar, auto-indent, line numbers, find/replace)
        proController = new EditorProFeaturesController(
                this,
                etCodeEditor,
                tvLineNumbers,
                layoutSymbols,
                tvStatusCursor,
                tvStatusLines,
                layoutSearchReplace,
                etSearchQuery,
                etReplaceQuery,
                tvSearchCount
        );

        // 3. Undo / Redo Manager
        undoRedoManager = new EditorUndoRedoManager(etCodeEditor);

        // 4. Tab Adapter
        tabAdapter = new EditorTabAdapter(new EditorTabAdapter.TabActionListener() {
            @Override
            public void onTabSelected(File file) {
                openFile(file);
            }

            @Override
            public void onTabClosed(File file) {
                closeFile(file);
            }
        });
        rvTabs.setAdapter(tabAdapter);
    }

    private void setupListeners() {
        // Drawer toggle
        btnDrawerToggle.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Search toggle
        btnActionSearch.setOnClickListener(v -> {
            boolean visible = proController.isFindReplaceVisible();
            proController.toggleFindReplace(!visible);
        });

        // Search action buttons
        findViewById(R.id.btn_search_next).setOnClickListener(v -> proController.findNext());
        findViewById(R.id.btn_search_prev).setOnClickListener(v -> proController.findPrevious());
        findViewById(R.id.btn_search_replace).setOnClickListener(v -> proController.replaceCurrent());
        findViewById(R.id.btn_search_replace_all).setOnClickListener(v -> proController.replaceAll());
        findViewById(R.id.btn_search_close).setOnClickListener(v -> proController.toggleFindReplace(false));

        // Undo / Redo
        btnActionUndo.setOnClickListener(v -> undoRedoManager.undo());
        btnActionRedo.setOnClickListener(v -> undoRedoManager.redo());

        // Save
        btnActionSave.setOnClickListener(v -> saveCurrentFile());

        // AI Assistant
        btnActionAi.setOnClickListener(v -> startActivity(new Intent(this, AIChatActivity.class)));

        // Overflow Menu
        btnActionOverflow.setOnClickListener(this::showOverflowMenu);

        // Real-time Debounced Syntax Highlighting
        etCodeEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                scheduleSyntaxHighlight();
            }
        });
    }

    private void scheduleSyntaxHighlight() {
        if (activeFile == null) return;
        if (syntaxRunnable != null) {
            syntaxHandler.removeCallbacks(syntaxRunnable);
        }
        syntaxRunnable = () -> {
            String ext = getFileExtension(activeFile.getName());
            JavaSyntaxHighlighter.highlight(etCodeEditor.getText(), ext);
        };
        syntaxHandler.postDelayed(syntaxRunnable, 300); // 300ms debounce
    }

    private void setupExplorerDrawer() {
        View drawerView = findViewById(R.id.drawer_layout_editor);
        File rootDir = new File(projectPath);

        explorerManager = new ProjectExplorerManager(this, drawerView, rootDir, file -> {
            openFile(file);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
    }

    private void openDefaultFile() {
        File rootDir = new File(projectPath);
        File mainActivity = findFileRecursive(rootDir, "MainActivity.java");
        if (mainActivity != null) {
            openFile(mainActivity);
        } else {
            File manifest = new File(rootDir, "app/src/main/AndroidManifest.xml");
            if (manifest.exists()) {
                openFile(manifest);
            }
        }
    }

    public void openFile(File file) {
        if (file == null || !file.exists() || file.isDirectory()) return;

        // Cache active file edits before switching
        if (activeFile != null) {
            fileCache.put(activeFile.getAbsolutePath(), etCodeEditor.getText().toString());
        }

        if (!openFiles.contains(file)) {
            openFiles.add(file);
        }

        activeFile = file;
        tvToolbarFile.setText(file.getName());

        String content = fileCache.get(file.getAbsolutePath());
        if (content == null) {
            content = readFile(file);
            fileCache.put(file.getAbsolutePath(), content);
        }

        etCodeEditor.setText(content);
        etCodeEditor.setVisibility(View.VISIBLE);
        tvEmptyEditor.setVisibility(View.GONE);

        String ext = getFileExtension(file.getName());
        tvStatusLang.setText(ext.toUpperCase());
        JavaSyntaxHighlighter.highlight(etCodeEditor.getText(), ext);

        tabAdapter.setTabs(openFiles, activeFile);
        proController.updateLineNumbersAndStatus();
        proController.updateCursorPosition();
    }

    public void closeFile(File file) {
        int index = openFiles.indexOf(file);
        if (index == -1) return;

        openFiles.remove(file);
        fileCache.remove(file.getAbsolutePath());

        if (file.equals(activeFile)) {
            if (!openFiles.isEmpty()) {
                int nextIndex = Math.max(0, index - 1);
                openFile(openFiles.get(nextIndex));
            } else {
                activeFile = null;
                etCodeEditor.setText("");
                etCodeEditor.setVisibility(View.GONE);
                tvEmptyEditor.setVisibility(View.VISIBLE);
                tvToolbarFile.setText("No file selected");
                tabAdapter.setTabs(openFiles, null);
            }
        } else {
            tabAdapter.setTabs(openFiles, activeFile);
        }
    }

    private void saveCurrentFile() {
        if (activeFile == null) {
            Toast.makeText(this, "No file open to save", Toast.LENGTH_SHORT).show();
            return;
        }

        String content = etCodeEditor.getText().toString();
        try {
            writeFile(activeFile, content);
            fileCache.put(activeFile.getAbsolutePath(), content);
            Toast.makeText(this, "Saved: " + activeFile.getName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showOverflowMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Zoom In (+)");
        popup.getMenu().add(0, 2, 0, "Zoom Out (-)");
        popup.getMenu().add(0, 3, 0, "Reset Zoom (100%)");
        popup.getMenu().add(0, 4, 0, "Export (.zip) Backup");
        popup.getMenu().add(0, 5, 0, "Settings");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    zoomController.zoomIn();
                    return true;
                case 2:
                    zoomController.zoomOut();
                    return true;
                case 3:
                    zoomController.resetZoom();
                    return true;
                case 4:
                    exportProject();
                    return true;
                case 5:
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void exportProject() {
        Toast.makeText(this, "Exporting project backup...", Toast.LENGTH_SHORT).show();
        ProjectMeta tempMeta = new ProjectMeta();
        tempMeta.setName(projectName != null ? projectName : "Project");
        tempMeta.setProjectPath(projectPath);

        ProjectExporter.exportProjectAsync(tempMeta, new ProjectExporter.ExportCallback() {
            @Override
            public void onSuccess(File zipFile) {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(EditorActivity.this)
                            .setTitle("Project Backup Created")
                            .setMessage("Saved to:\n" + zipFile.getAbsolutePath())
                            .setPositiveButton("OK", null)
                            .show();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(EditorActivity.this, "Export failed: " + errorMessage, Toast.LENGTH_LONG).show());
            }
        });
    }

    private File findFileRecursive(File dir, String filename) {
        File[] list = dir.listFiles();
        if (list == null) return null;
        for (File f : list) {
            if (f.isDirectory()) {
                File found = findFileRecursive(f, filename);
                if (found != null) return found;
            } else if (f.getName().equals(filename)) {
                return f;
            }
        }
        return null;
    }

    private String getFileExtension(String name) {
        int idx = name.lastIndexOf('.');
        return idx != -1 ? name.substring(idx + 1) : "";
    }

    private String readFile(File file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "// Unable to read file: " + e.getMessage();
        }
    }

    private void writeFile(File file, String content) throws Exception {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content);
            writer.flush();
        }
    }
}
