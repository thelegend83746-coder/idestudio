package com.ide.studio.model;

import java.io.File;

public class Project {
    private String name;
    private String packageName;
    private String path;
    private int minSdk;
    private int targetSdk;
    private String templateType;

    public Project(String name, String packageName, String path) {
        this.name = name;
        this.packageName = packageName;
        this.path = path;
        this.minSdk = 21;
        this.targetSdk = 30;
        this.templateType = "Simple App";
    }

    public Project(String name, String packageName, String path, int minSdk, int targetSdk, String templateType) {
        this.name = name;
        this.packageName = packageName;
        this.path = path;
        this.minSdk = minSdk;
        this.targetSdk = targetSdk;
        this.templateType = templateType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public File getProjectDir() {
        return new File(path);
    }

    public File getRootDirectory() {
        return getProjectDir();
    }

    public File getManifestFile() {
        return new File(getProjectDir(), "app/src/main/AndroidManifest.xml");
    }

    public int getMinSdk() {
        return minSdk;
    }

    public void setMinSdk(int minSdk) {
        this.minSdk = minSdk;
    }

    public int getTargetSdk() {
        return targetSdk;
    }

    public void setTargetSdk(int targetSdk) {
        this.targetSdk = targetSdk;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }
}
