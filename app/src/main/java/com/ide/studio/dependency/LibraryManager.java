package com.ide.studio.dependency;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Project Library Manager: tracks compiled JAR and AAR dependencies in project app/libs/
 */
public class LibraryManager {

    private final File projectLibsDir;
    private final List<LibraryModel> libraries = new ArrayList<>();

    public LibraryManager(File projectRootDir) {
        File appDir = new File(projectRootDir, "app");
        if (appDir.exists()) {
            this.projectLibsDir = new File(appDir, "libs");
        } else {
            this.projectLibsDir = new File(projectRootDir, "libs");
        }
        this.projectLibsDir.mkdirs();
    }

    public File getLibsDirectory() {
        return projectLibsDir;
    }

    public List<File> getJarFiles() {
        List<File> jars = new ArrayList<>();
        File[] files = projectLibsDir.listFiles((d, name) -> name.endsWith(".jar"));
        if (files != null) {
            for (File f : files) jars.add(f);
        }
        return jars;
    }

    public List<LibraryModel> getLibraries() {
        return libraries;
    }

    public void addLibrary(LibraryModel lib) {
        libraries.add(lib);
    }
}
