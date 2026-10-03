package com.idestudio.app.data.models;

import java.io.Serializable;
import java.util.UUID;

public class ProjectMeta implements Serializable {
    private String id;
    private String name;
    private String packageName;
    private String projectPath;
    private int minSdk;
    private int targetSdk;
    private long createdAt;
    private long lastModified;
    private String templateName;
    private String language; // Always "Java"

    public ProjectMeta() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
        this.language = "Java";
    }

    public ProjectMeta(String name, String packageName, String projectPath, int minSdk, int targetSdk, String templateName) {
        this();
        this.name = name;
        this.packageName = packageName;
        this.projectPath = projectPath;
        this.minSdk = minSdk;
        this.targetSdk = targetSdk;
        this.templateName = templateName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
