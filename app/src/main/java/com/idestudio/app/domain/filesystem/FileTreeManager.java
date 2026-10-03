package com.idestudio.app.domain.filesystem;

import com.idestudio.app.data.models.FileNode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FileTreeManager {

    private final File projectRoot;
    private FileNode rootNode;

    public FileTreeManager(File projectRoot) {
        this.projectRoot = projectRoot;
        reload();
    }

    public void reload() {
        if (projectRoot.exists() && projectRoot.isDirectory()) {
            rootNode = buildNodeRecursive(projectRoot, 0);
            if (rootNode != null) {
                rootNode.setExpanded(true); // Root is expanded by default
            }
        }
    }

    public FileNode getRootNode() {
        return rootNode;
    }

    private FileNode buildNodeRecursive(File file, int depth) {
        FileNode node = new FileNode(file, depth);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                // Sort directories first, then alphabetically
                Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        if (f1.isDirectory() && !f2.isDirectory()) return -1;
                        if (!f1.isDirectory() && f2.isDirectory()) return 1;
                        return f1.getName().compareToIgnoreCase(f2.getName());
                    }
                });

                for (File child : files) {
                    // Filter out hidden VCS / build folders if needed, or keep standard
                    if (!child.getName().startsWith(".")) {
                        node.addChild(buildNodeRecursive(child, depth + 1));
                    }
                }
            }
        }
        return node;
    }

    /**
     * Returns a flat list of all currently visible (expanded) nodes for the RecyclerView.
     */
    public List<FileNode> getVisibleNodes() {
        List<FileNode> visible = new ArrayList<>();
        if (rootNode != null) {
            collectVisibleNodes(rootNode, visible);
        }
        return visible;
    }

    private void collectVisibleNodes(FileNode node, List<FileNode> visibleList) {
        visibleList.add(node);
        if (node.isDirectory() && node.isExpanded()) {
            for (FileNode child : node.getChildren()) {
                collectVisibleNodes(child, visibleList);
            }
        }
    }
}
