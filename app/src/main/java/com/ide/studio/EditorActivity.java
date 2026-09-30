package com.ide.studio;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
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
import java.util.Arrays;
import java.util.List;

/**
 * Main IDE Code Editor activity.
 * Integrates smooth kinetic overscroll editor, navigation drawer file explorer,
 * multiple file tabs, fast undo/redo, auto-save, symbols/autocomplete accessory bar,
 * reference-style 3-dots menu (Java, Res, Asset, Lib, JNI, Local Library, Build, Build AI),
 * and offline compilation engine with "Fix with AI" error recovery.
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
    private LinearLayout mLayoutSymbolBar;
    private LinearLayout mPillSaveFile;
    private RecyclerView mRecyclerFileTree;
    private FileTreeAdapter mFileTreeAdapter;

    private Project mCurrentProject;
    private File mCurrentOpenFile;
    private final List<File> mOpenFiles = new ArrayList<>();
    private boolean mHasUnsavedChanges = false;
    private PreferencesManager mPrefs;

    // Autocomplete dictionary for Java and Android
    private static final String[] AUTOCOMPLETE_KEYWORDS = new String[]{
            "public", "private", "protected", "static", "final", "void", "class", "extends", "implements",
            "import", "package", "return", "new", "this", "super", "if", "else", "for", "while", "do",
            "try", "catch", "finally", "throw", "throws", "boolean", "int", "float", "double", "long", "String",
            "Activity", "AppCompatActivity", "Bundle", "Override", "View", "Button", "TextView", "EditText",
            "ImageView", "LinearLayout", "RelativeLayout", "FrameLayout", "Toast", "Intent", "Color", "Paint",
            "Canvas", "R.layout", "R.id", "R.string", "R.color", "setContentView", "findViewById", "setOnClickListener"
    };

    private static final String[] QUICK_SYMBOLS = new String[]{
            "Tab", "{", "}", "(", ")", "[", "]", ";", "\"", "=", ".", ",", "<", ">", "//"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        mPrefs = new PreferencesManager(this);

        initViews();
        resolveProject();
        setupEditor();
        setupSymbolAndAutocompleteBar();
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
        mLayoutSymbolBar = findViewById(R.id.layout_symbol_bar);
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
        mEditor.setHighlightCurrentLine(mPrefs.isHighlightCurrentLine());
        mEditor.applyTheme(mPrefs.isDarkEditorTheme());

        mEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mCurrentOpenFile != null && !mHasUnsavedChanges) {
                    mHasUnsavedChanges = true;
                    mPillSaveFile.setVisibility(View.VISIBLE);
                }
                updateAutocompleteSuggestions();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        mPillSaveFile.setOnClickListener(v -> saveCurrentFile());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mEditor != null) {
            mEditor.setLineNumbersVisible(mPrefs.showLineNumbers());
            mEditor.setTextSize(mPrefs.getEditorFontSize());
            mEditor.setHighlightCurrentLine(mPrefs.isHighlightCurrentLine());
            mEditor.applyTheme(mPrefs.isDarkEditorTheme());
        }
    }

    private void setupSymbolAndAutocompleteBar() {
        refreshSymbolBar("");
    }

    private void refreshSymbolBar(String prefix) {
        if (mLayoutSymbolBar == null) return;
        mLayoutSymbolBar.removeAllViews();

        float density = getResources().getDisplayMetrics().density;

        // 1. Add matching Autocomplete chips if prefix >= 2
        if (prefix != null && prefix.length() >= 2 && mPrefs.isAutoCompleteEnabled()) {
            int matchCount = 0;
            for (String kw : AUTOCOMPLETE_KEYWORDS) {
                if (kw.toLowerCase().startsWith(prefix.toLowerCase()) && !kw.equalsIgnoreCase(prefix)) {
                    Button chip = new Button(this);
                    chip.setText(kw);
                    chip.setTextSize(12);
                    chip.setTextColor(Color.parseColor("#5844ED"));
                    chip.setBackgroundResource(R.drawable.bg_input_outline);
                    chip.setPadding((int) (10 * density), 0, (int) (10 * density), 0);
                    chip.setAllCaps(false);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, (int) (32 * density));
                    lp.setMarginEnd((int) (6 * density));
                    chip.setLayoutParams(lp);

                    chip.setOnClickListener(v -> {
                        mEditor.replaceCurrentWord(kw);
                        refreshSymbolBar("");
                    });

                    mLayoutSymbolBar.addView(chip);
                    matchCount++;
                    if (matchCount >= 4) break;
                }
            }
        }

        // 2. Add quick programming symbols
        for (String sym : QUICK_SYMBOLS) {
            Button btn = new Button(this);
            btn.setText(sym);
            btn.setTextSize(13);
            btn.setTextColor(Color.parseColor("#374151"));
            btn.setBackgroundResource(R.drawable.bg_card_rounded);
            btn.setPadding((int) (8 * density), 0, (int) (8 * density), 0);
            btn.setAllCaps(false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, (int) (32 * density));
            lp.setMarginEnd((int) (4 * density));
            btn.setLayoutParams(lp);

            btn.setOnClickListener(v -> {
                if ("Tab".equals(sym)) {
                    mEditor.insertTab(mPrefs.getEditorTabSize());
                } else if ("{".equals(sym)) {
                    mEditor.insertText("{}");
                    mEditor.setSelection(mEditor.getSelectionStart() - 1);
                } else if ("(".equals(sym)) {
                    mEditor.insertText("()");
                    mEditor.setSelection(mEditor.getSelectionStart() - 1);
                } else if ("[".equals(sym)) {
                    mEditor.insertText("[]");
                    mEditor.setSelection(mEditor.getSelectionStart() - 1);
                } else if ("\"".equals(sym)) {
                    mEditor.insertText("\"\"");
                    mEditor.setSelection(mEditor.getSelectionStart() - 1);
                } else {
                    mEditor.insertText(sym);
                }
            });

            mLayoutSymbolBar.addView(btn);
        }
    }

    private void updateAutocompleteSuggestions() {
        String prefix = mEditor.getCurrentWordPrefix();
        refreshSymbolBar(prefix);
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
        popup.getMenu().add(0, 2, 1, "Java File");
        popup.getMenu().add(0, 3, 2, "Resources File");
        popup.getMenu().add(0, 4, 3, "Assets File");
        popup.getMenu().add(0, 5, 4, "Lib File");
        popup.getMenu().add(0, 6, 5, "JNI File");
        popup.getMenu().add(0, 7, 6, "Local Library");
        popup.getMenu().add(0, 8, 7, "Build");
        popup.getMenu().add(0, 9, 8, "Settings");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    Intent aiIntent = new Intent(EditorActivity.this, BuildAiActivity.class);
                    aiIntent.putExtra("project_path", mCurrentProject.getRootDirectory().getAbsolutePath());
                    startActivity(aiIntent);
                    return true;
                case 2:
                    showCreateJavaFileDialog();
                    return true;
                case 3:
                    showCreateResourceFileDialog();
                    return true;
                case 4:
                    showCreateAssetFileDialog();
                    return true;
                case 5:
                    showLibFilesDialog();
                    return true;
                case 6:
                    showJniFileDialog();
                    return true;
                case 7:
                    showLocalLibraryDialog();
                    return true;
                case 8:
                    startBuildPipeline();
                    return true;
                case 9:
                    startActivity(new Intent(EditorActivity.this, SettingsActivity.class));
                    return true;
            }
            return false;
        });

        popup.show();
    }

    private void showCreateJavaFileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Java File");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText etName = new EditText(this);
        etName.setHint("Class / File Name (e.g. MyHelper)");
        layout.addView(etName);

        final EditText etPackage = new EditText(this);
        etPackage.setHint("Package Name");
        etPackage.setText(mCurrentProject.getPackageName());
        layout.addView(etPackage);

        builder.setView(layout);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String pkg = etPackage.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Class name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!name.endsWith(".java")) {
                name += ".java";
            }
            String className = name.replace(".java", "");
            String pkgPath = pkg.replace('.', '/');
            File dir = new File(mCurrentProject.getRootDirectory(), "app/src/main/java/" + pkgPath);
            dir.mkdirs();
            File newJavaFile = new File(dir, name);

            String template = "package " + pkg + ";\n\n" +
                    "public class " + className + " {\n\n" +
                    "    public " + className + "() {\n" +
                    "    }\n" +
                    "}\n";
            FileUtils.writeFile(newJavaFile, template);
            mFileTreeAdapter.notifyDataSetChanged();
            openFile(newJavaFile);
            Toast.makeText(this, "Created: " + name, Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showCreateResourceFileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Resource File");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final Spinner spinnerType = new Spinner(this);
        String[] types = new String[]{"layout", "drawable", "values", "menu"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(adapter);
        layout.addView(spinnerType);

        final EditText etName = new EditText(this);
        etName.setHint("File Name (e.g. custom_view.xml)");
        layout.addView(etName);

        builder.setView(layout);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String resType = (String) spinnerType.getSelectedItem();
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "File name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!name.endsWith(".xml")) {
                name += ".xml";
            }
            File dir = new File(mCurrentProject.getRootDirectory(), "app/src/main/res/" + resType);
            dir.mkdirs();
            File newResFile = new File(dir, name);

            String template;
            if ("layout".equals(resType)) {
                template = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "    android:layout_width=\"match_parent\"\n" +
                        "    android:layout_height=\"match_parent\"\n" +
                        "    android:orientation=\"vertical\">\n\n" +
                        "</LinearLayout>\n";
            } else if ("values".equals(resType)) {
                template = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n</resources>\n";
            } else {
                template = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<menu xmlns:android=\"http://schemas.android.com/apk/res/android\">\n</menu>\n";
            }

            FileUtils.writeFile(newResFile, template);
            mFileTreeAdapter.notifyDataSetChanged();
            openFile(newResFile);
            Toast.makeText(this, "Created: " + name, Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showCreateAssetFileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Asset File");

        final EditText etName = new EditText(this);
        etName.setHint("File Name (e.g. data.json)");
        builder.setView(etName);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                File dir = new File(mCurrentProject.getRootDirectory(), "app/src/main/assets");
                dir.mkdirs();
                File asset = new File(dir, name);
                FileUtils.writeFile(asset, "");
                mFileTreeAdapter.notifyDataSetChanged();
                openFile(asset);
                Toast.makeText(this, "Created asset: " + name, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showLibFilesDialog() {
        File libsDir = new File(mCurrentProject.getRootDirectory(), "app/libs");
        libsDir.mkdirs();
        File[] files = libsDir.listFiles();
        StringBuilder sb = new StringBuilder();
        if (files != null && files.length > 0) {
            for (File f : files) {
                sb.append("• ").append(f.getName()).append(" (").append(f.length() / 1024).append(" KB)\n");
            }
        } else {
            sb.append("No library files in app/libs/\nPlace .jar or .aar files in app/libs/ to link them.");
        }

        new AlertDialog.Builder(this)
                .setTitle("Library Files (app/libs)")
                .setMessage(sb.toString().trim())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showJniFileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Native JNI File");

        final EditText etName = new EditText(this);
        etName.setHint("File Name (e.g. native-lib.cpp)");
        builder.setView(etName);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                File dir = new File(mCurrentProject.getRootDirectory(), "app/src/main/jni");
                dir.mkdirs();
                File jni = new File(dir, name);
                String boilerplate = "#include <jni.h>\n#include <string>\n\n// Native C/C++ source\n";
                FileUtils.writeFile(jni, boilerplate);
                mFileTreeAdapter.notifyDataSetChanged();
                openFile(jni);
                Toast.makeText(this, "Created JNI source: " + name, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showLocalLibraryDialog() {
        File gradle = new File(mCurrentProject.getRootDirectory(), "app/build.gradle");
        String content = FileUtils.readFile(gradle);
        new AlertDialog.Builder(this)
                .setTitle("Project Dependencies")
                .setMessage("Build script configuration:\n\n" + (content.isEmpty() ? "No dependencies configured." : content))
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
        if (mHasUnsavedChanges || mPrefs.isAutoSaveBeforeBuild()) {
            saveCurrentFile();
        }

        BuildLogDialog logDialog = new BuildLogDialog(this);
        logDialog.show();

        logDialog.setOnFixWithAiListener(errorLogs -> {
            Intent aiIntent = new Intent(EditorActivity.this, BuildAiActivity.class);
            aiIntent.putExtra("project_path", mCurrentProject.getRootDirectory().getAbsolutePath());
            aiIntent.putExtra("fix_build_error", errorLogs);
            startActivity(aiIntent);
        });

        ApkBuildPipeline.buildProject(this, mCurrentProject, logDialog, (success, apkFile) -> {
            runOnUiThread(() -> {
                if (success && apkFile != null) {
                    Toast.makeText(this, "Build Succeeded!", Toast.LENGTH_SHORT).show();
                    ApkBuildPipeline.installApk(this, apkFile);
                } else {
                    logDialog.setFixWithAiVisible(true);
                    Toast.makeText(this, "Build Failed! Use 'Fix with AI' or check logs.", Toast.LENGTH_LONG).show();
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
