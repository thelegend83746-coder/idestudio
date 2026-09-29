package com.apk.builder.model;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Library {
    private String mLibraryName;
    private File mPath;
    private static final Pattern mPackagePattern = Pattern.compile("package=\"([^\"]+)\"");

    public Library(File path) {
        this.mPath = path;
        this.mLibraryName = path != null ? path.getName() : "";
    }

    public static Library fromFile(File path) {
        return new Library(path);
    }

    public String getName() {
        return mLibraryName;
    }

    public File getPath() {
        return mPath;
    }

    public File getClassJarFile() {
        if (mPath == null) return null;
        if (mPath.isFile() && mPath.getName().endsWith(".jar")) {
            return mPath;
        }
        File classesJar = new File(mPath, "classes.jar");
        if (classesJar.exists()) {
            return classesJar;
        }
        return mPath;
    }

    public List<File> getDexFiles() {
        List<File> list = new ArrayList<>();
        if (mPath == null) return list;
        if (mPath.isDirectory()) {
            File[] files = mPath.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().endsWith(".dex")) {
                        list.add(f);
                    }
                }
            }
        }
        return list;
    }

    public File getJniFile() {
        if (mPath == null) return null;
        File jni = new File(mPath, "jni");
        return jni.exists() ? jni : null;
    }

    public File getResourcesFile() {
        if (mPath == null) return null;
        File res = new File(mPath, "res");
        return res.exists() ? res : null;
    }

    public boolean requiresResourceFile() {
        File res = getResourcesFile();
        return res != null && res.exists() && res.isDirectory();
    }

    public String getPackageName() {
        if (mPath == null) return "";
        File manifest = new File(mPath, "AndroidManifest.xml");
        if (manifest.exists()) {
            try (FileInputStream fis = new FileInputStream(manifest)) {
                byte[] data = new byte[(int) Math.min(manifest.length(), 4096)];
                int read = fis.read(data);
                String content = new String(data, 0, read);
                Matcher matcher = mPackagePattern.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception ignored) {
            }
        }
        return "";
    }
}
