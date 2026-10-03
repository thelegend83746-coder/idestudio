package com.idestudio.app.data.models;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FileNode implements Serializable {
    private final File file;
    private final String name;
    private final String path;
    private final boolean isDirectory;
    private final int depth;
    private boolean isExpanded;
    private final List<FileNode> children = new ArrayList<>();

    public FileNode(File file, int depth) {
        this.file = file;
        this.name = file.getName();
        this.path = file.getAbsolutePath();
        this.isDirectory = file.isDirectory();
        this.depth = depth;
        this.isExpanded = false;
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public List<FileNode> getChildren() {
        return children;
    }

    public void addChild(FileNode child) {
        children.add(child);
    }
}
