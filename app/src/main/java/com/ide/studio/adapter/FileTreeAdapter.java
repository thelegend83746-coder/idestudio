package com.ide.studio.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ide.studio.R;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * File tree adapter for IDE drawer navigation.
 * Renders nested folders and files with expand/collapse capability.
 */
public class FileTreeAdapter extends RecyclerView.Adapter<FileTreeAdapter.FileNodeViewHolder> {

    public interface OnFileSelectedListener {
        void onFileSelected(File file);
    }

    public static class FileNode {
        public final File file;
        public final int depth;
        public boolean isDirectory;

        public FileNode(File file, int depth) {
            this.file = file;
            this.depth = depth;
            this.isDirectory = file.isDirectory();
        }
    }

    private final Context mContext;
    private final OnFileSelectedListener mListener;
    private File mRootDirectory;
    private final List<FileNode> mVisibleNodes = new ArrayList<>();
    private final Set<String> mExpandedPaths = new HashSet<>();

    public FileTreeAdapter(Context context, OnFileSelectedListener listener) {
        this.mContext = context;
        this.mListener = listener;
    }

    public void setRootDirectory(File rootDirectory) {
        this.mRootDirectory = rootDirectory;
        mExpandedPaths.clear();
        if (rootDirectory != null && rootDirectory.exists()) {
            mExpandedPaths.add(rootDirectory.getAbsolutePath());
            // Also expand app and app/src if they exist
            File appDir = new File(rootDirectory, "app");
            if (appDir.exists()) mExpandedPaths.add(appDir.getAbsolutePath());
            File srcDir = new File(appDir, "src");
            if (srcDir.exists()) mExpandedPaths.add(srcDir.getAbsolutePath());
            File mainDir = new File(srcDir, "main");
            if (mainDir.exists()) mExpandedPaths.add(mainDir.getAbsolutePath());
        }
        rebuildVisibleNodes();
    }

    private void rebuildVisibleNodes() {
        mVisibleNodes.clear();
        if (mRootDirectory != null && mRootDirectory.exists()) {
            traverseDirectory(mRootDirectory, 0);
        }
        notifyDataSetChanged();
    }

    private void traverseDirectory(File dir, int depth) {
        File[] children = dir.listFiles();
        if (children == null) return;

        List<File> sorted = new ArrayList<>();
        for (File child : children) {
            // Ignore hidden files and build output cache in drawer
            if (child.getName().startsWith(".") && !child.getName().equals(".build_studio")) {
                continue;
            }
            if (child.getName().equals("build") && depth <= 2) {
                continue;
            }
            sorted.add(child);
        }

        Collections.sort(sorted, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        for (File child : sorted) {
            mVisibleNodes.add(new FileNode(child, depth));
            if (child.isDirectory() && mExpandedPaths.contains(child.getAbsolutePath())) {
                traverseDirectory(child, depth + 1);
            }
        }
    }

    @NonNull
    @Override
    public FileNodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_file_tree, parent, false);
        return new FileNodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileNodeViewHolder holder, int position) {
        FileNode node = mVisibleNodes.get(position);

        // Indentation calculation
        int indentPixels = (int) (node.depth * 16 * mContext.getResources().getDisplayMetrics().density);
        ViewGroup.LayoutParams params = holder.viewIndent.getLayoutParams();
        params.width = indentPixels;
        holder.viewIndent.setLayoutParams(params);

        holder.tvName.setText(node.file.getName());

        if (node.isDirectory) {
            holder.ivIcon.setImageResource(R.drawable.ic_folder);
            holder.tvName.setTextColor(Color.parseColor("#111827"));
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_code_file);
            holder.tvName.setTextColor(Color.parseColor("#374151"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (node.isDirectory) {
                String path = node.file.getAbsolutePath();
                if (mExpandedPaths.contains(path)) {
                    mExpandedPaths.remove(path);
                } else {
                    mExpandedPaths.add(path);
                }
                rebuildVisibleNodes();
            } else {
                if (mListener != null) {
                    mListener.onFileSelected(node.file);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mVisibleNodes.size();
    }

    static class FileNodeViewHolder extends RecyclerView.ViewHolder {
        View viewIndent;
        ImageView ivIcon;
        TextView tvName;

        public FileNodeViewHolder(@NonNull View itemView) {
            super(itemView);
            viewIndent = itemView.findViewById(R.id.view_indent);
            ivIcon = itemView.findViewById(R.id.iv_file_icon);
            tvName = itemView.findViewById(R.id.tv_file_name);
        }
    }
}
