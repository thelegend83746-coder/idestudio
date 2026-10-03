package com.idestudio.app.domain.project;

import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages project persistence and indexing.
 * Automatically creates and uses /storage/emulated/0/idestudio as the root workspace.
 */
public class LocalProjectStore {

    private static final String TAG = "LocalProjectStore";
    public static final String IDESTUDIO_ROOT = "/storage/emulated/0/idestudio";

    private static LocalProjectStore instance;
    private final Map<String, ProjectMeta> projectCache = new LinkedHashMap<>();
    private final File idestudioBaseDir;
    private final File metadataFile;

    private LocalProjectStore() {
        idestudioBaseDir = getResolvedBaseDir();
        ensureDirectoryExists(idestudioBaseDir);

        File metaDir = new File(idestudioBaseDir, ".metadata");
        ensureDirectoryExists(metaDir);
        metadataFile = new File(metaDir, "projects.json");

        // Also ensure backups folder exists
        File backupsDir = new File(idestudioBaseDir, "backups");
        ensureDirectoryExists(backupsDir);

        loadProjects();
    }

    public static synchronized LocalProjectStore getInstance() {
        if (instance == null) {
            instance = new LocalProjectStore();
        }
        return instance;
    }

    public static File getResolvedBaseDir() {
        File preferred = new File(IDESTUDIO_ROOT);
        if (preferred.exists() || preferred.mkdirs()) {
            return preferred;
        }
        File sdcard = Environment.getExternalStorageDirectory();
        File fallback = new File(sdcard, "idestudio");
        if (!fallback.exists()) {
            fallback.mkdirs();
        }
        return fallback;
    }

    public static String getBaseDirPath() {
        return getResolvedBaseDir().getAbsolutePath();
    }

    private void ensureDirectoryExists(File dir) {
        if (dir != null && !dir.exists()) {
            boolean created = dir.mkdirs();
            Log.d(TAG, "Directory " + dir.getAbsolutePath() + " created: " + created);
        }
    }

    public synchronized List<ProjectMeta> getAllProjects() {
        // Sync with disk to catch any projects created or imported directly in /storage/emulated/0/idestudio
        scanDirectoryForProjects();
        List<ProjectMeta> list = new ArrayList<>(projectCache.values());
        Collections.sort(list, (a, b) -> Long.compare(b.getLastModified(), a.getLastModified()));
        return list;
    }

    public synchronized ProjectMeta getProjectById(String id) {
        if (id == null) return null;
        ProjectMeta p = projectCache.get(id);
        if (p == null) {
            scanDirectoryForProjects();
            p = projectCache.get(id);
        }
        return p;
    }

    public synchronized void saveProject(ProjectMeta project) {
        if (project == null || project.getId() == null) return;
        projectCache.put(project.getId(), project);
        persistToDisk();
    }

    public synchronized void deleteProject(String id) {
        ProjectMeta project = projectCache.remove(id);
        if (project != null && project.getProjectPath() != null) {
            File projectDir = new File(project.getProjectPath());
            deleteRecursive(projectDir);
        }
        persistToDisk();
    }

    private void scanDirectoryForProjects() {
        if (!idestudioBaseDir.exists() || !idestudioBaseDir.isDirectory()) return;

        File[] files = idestudioBaseDir.listFiles();
        if (files == null) return;

        for (File dir : files) {
            if (dir.isDirectory() && !dir.getName().startsWith(".") && !dir.getName().equalsIgnoreCase("backups")) {
                File manifest = new File(dir, "app/src/main/AndroidManifest.xml");
                File gradle = new File(dir, "app/build.gradle");

                if (manifest.exists() || gradle.exists()) {
                    String id = "proj_" + Math.abs(dir.getAbsolutePath().hashCode());
                    if (!projectCache.containsKey(id)) {
                        ProjectMeta meta = new ProjectMeta();
                        meta.setId(id);
                        meta.setName(dir.getName());
                        meta.setProjectPath(dir.getAbsolutePath());
                        meta.setPackageName("com.example." + dir.getName().toLowerCase().replaceAll("[^a-z0-9]", ""));
                        meta.setLastModified(dir.lastModified());
                        meta.setTemplateType("Empty Activity");
                        meta.setMinSdkVersion(21);
                        meta.setTargetSdkVersion(34);
                        projectCache.put(id, meta);
                    }
                }
            }
        }
    }

    private synchronized void loadProjects() {
        projectCache.clear();
        if (!metadataFile.exists()) {
            scanDirectoryForProjects();
            persistToDisk();
            return;
        }

        try {
            String jsonStr = readFile(metadataFile);
            if (jsonStr != null && !jsonStr.trim().isEmpty()) {
                JSONArray arr = new JSONArray(jsonStr);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    ProjectMeta meta = ProjectMeta.fromJson(obj);
                    if (meta != null && meta.getProjectPath() != null) {
                        File pDir = new File(meta.getProjectPath());
                        if (pDir.exists()) {
                            projectCache.put(meta.getId(), meta);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading project metadata: " + e.getMessage(), e);
        }

        scanDirectoryForProjects();
    }

    private synchronized void persistToDisk() {
        try {
            JSONArray arr = new JSONArray();
            for (ProjectMeta p : projectCache.values()) {
                arr.put(p.toJson());
            }
            writeFile(metadataFile, arr.toString(2));
        } catch (Exception e) {
            Log.e(TAG, "Error saving project metadata: " + e.getMessage(), e);
        }
    }

    private void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
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
            return null;
        }
    }

    private void writeFile(File file, String content) {
        try {
            ensureDirectoryExists(file.getParentFile());
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                writer.write(content);
                writer.flush();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed writing file: " + file.getAbsolutePath(), e);
        }
    }
}
