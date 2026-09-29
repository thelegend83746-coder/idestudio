package com.ide.studio.dependency;

public class LibraryModel {
    private String groupId;
    private String artifactId;
    private String version;
    private String packaging; // "jar" or "aar"
    private boolean downloaded;

    public LibraryModel(String groupId, String artifactId, String version) {
        this(groupId, artifactId, version, "jar");
    }

    public LibraryModel(String groupId, String artifactId, String version, String packaging) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packaging = packaging != null ? packaging : "jar";
        this.downloaded = false;
    }

    public String getGroupId() { return groupId; }
    public String getArtifactId() { return artifactId; }
    public String getVersion() { return version; }
    public String getPackaging() { return packaging; }
    public boolean isDownloaded() { return downloaded; }
    public void setDownloaded(boolean downloaded) { this.downloaded = downloaded; }

    public String getCoordinates() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
