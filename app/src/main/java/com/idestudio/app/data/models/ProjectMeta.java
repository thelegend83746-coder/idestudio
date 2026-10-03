package com.idestudio.app.data.models;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.UUID;

public class ProjectMeta implements Serializable {
    private String id;
    private String name;
    private String packageName;
    private String projectPath;
    private int minSdk = 21;
    private int targetSdk = 34;
    private long createdAt;
    private long lastModified;
    private String templateName = "Empty Activity";
    private String language = "Java";

    public ProjectMeta() {
        this.id = "proj_" + UUID.randomUUID().toString();
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

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("name", name);
            obj.put("packageName", packageName);
            obj.put("projectPath", projectPath);
            obj.put("minSdk", minSdk);
            obj.put("targetSdk", targetSdk);
            obj.put("createdAt", createdAt);
            obj.put("lastModified", lastModified);
            obj.put("templateName", templateName);
            obj.put("language", language);
        } catch (Exception ignored) {}
        return obj;
    }

    public static ProjectMeta fromJson(JSONObject obj) {
        if (obj == null) return null;
        ProjectMeta meta = new ProjectMeta();
        meta.setId(obj.optString("id", meta.getId()));
        meta.setName(obj.optString("name", ""));
        meta.setPackageName(obj.optString("packageName", ""));
        meta.setProjectPath(obj.optString("projectPath", ""));
        meta.setMinSdk(obj.optInt("minSdk", 21));
        meta.setTargetSdk(obj.optInt("targetSdk", 34));
        meta.setCreatedAt(obj.optLong("createdAt", System.currentTimeMillis()));
        meta.setLastModified(obj.optLong("lastModified", System.currentTimeMillis()));
        meta.setTemplateName(obj.optString("templateName", "Empty Activity"));
        meta.setLanguage(obj.optString("language", "Java"));
        return meta;
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

    public int getMinSdkVersion() {
        return minSdk;
    }

    public void setMinSdkVersion(int minSdk) {
        this.minSdk = minSdk;
    }

    public int getTargetSdk() {
        return targetSdk;
    }

    public void setTargetSdk(int targetSdk) {
        this.targetSdk = targetSdk;
    }

    public int getTargetSdkVersion() {
        return targetSdk;
    }

    public void setTargetSdkVersion(int targetSdk) {
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

    public String getTemplateType() {
        return templateName;
    }

    public void setTemplateType(String templateType) {
        this.templateName = templateType;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
