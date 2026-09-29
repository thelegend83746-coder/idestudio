package com.apk.builder.model;

import com.apk.builder.logger.Logger;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Project {
    private File mAssetsFile;
    private File mJavaFile;
    private String mJavaVersion = "1.8";
    private List<Library> mLibraries = new ArrayList<>();
    private Logger mLogger = new Logger();
    private File mManifestFile;
    private int mMinSdk = 21;
    private File mOutputFile;
    private File mResourcesFile;
    private int mTargetSdk = 33;
    private int mVersionCode = 1;
    private String mVersionName = "1.0.0";

    public File getAssetsFile() {
        return mAssetsFile;
    }

    public void setAssetsFile(File assetsFile) {
        this.mAssetsFile = assetsFile;
    }

    public File getJavaFile() {
        return mJavaFile;
    }

    public void setJavaFile(File javaFile) {
        this.mJavaFile = javaFile;
    }

    public String getJavaVersion() {
        return mJavaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.mJavaVersion = javaVersion;
    }

    public List<Library> getLibraries() {
        return mLibraries;
    }

    public void setLibraries(List<Library> libraries) {
        this.mLibraries = libraries != null ? libraries : new ArrayList<>();
    }

    public Logger getLogger() {
        return mLogger;
    }

    public void setLogger(Logger logger) {
        this.mLogger = logger != null ? logger : new Logger();
    }

    public File getManifestFile() {
        return mManifestFile;
    }

    public void setManifestFile(File manifestFile) {
        this.mManifestFile = manifestFile;
    }

    public int getMinSdk() {
        return mMinSdk;
    }

    public void setMinSdk(int minSdk) {
        this.mMinSdk = minSdk;
    }

    public File getOutputFile() {
        return mOutputFile;
    }

    public void setOutputFile(File outputFile) {
        this.mOutputFile = outputFile;
    }

    public File getResourcesFile() {
        return mResourcesFile;
    }

    public void setResourcesFile(File resourcesFile) {
        this.mResourcesFile = resourcesFile;
    }

    public int getTargetSdk() {
        return mTargetSdk;
    }

    public void setTargetSdk(int targetSdk) {
        this.mTargetSdk = targetSdk;
    }

    public int getVersionCode() {
        return mVersionCode;
    }

    public void setVersionCode(int versionCode) {
        this.mVersionCode = versionCode;
    }

    public String getVersionName() {
        return mVersionName;
    }

    public void setVersionName(String versionName) {
        this.mVersionName = versionName;
    }
}
