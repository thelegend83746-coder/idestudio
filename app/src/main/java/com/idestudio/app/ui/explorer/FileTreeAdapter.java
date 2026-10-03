package com.idestudio.app.ui.explorer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.idestudio.app.R;
import com.idestudio.app.data.models.FileNode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileTreeAdapter extends RecyclerView.Adapter<FileTreeAdapter.FileTreeNodeViewHolder> {

    public interface OnFileTreeInteractionListener {
        void onFileClicked(File file);
        void onFolderClicked(FileNode folderNode);
        void onFileLongClicked(File file);
        void onFolderLongClicked(File folder);
    }

    private final List<FileNode> visibleNodes = new ArrayList<>();
    private final OnFileTreeInteractionListener listener;

    public FileTreeAdapter(OnFileTreeInteractionListener listener) {
        this.listener = listener;
    }

    public void setNodes(List<FileNode> nodes) {
        visibleNodes.clear();
        if (nodes != null) {
            visibleNodes.addAll(nodes);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FileTreeNodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_tree_node, parent, false);
        return new FileTreeNodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileTreeNodeViewHolder holder, int position) {
        FileNode node = visibleNodes.get(position);
        holder.bind(node);
    }

    @Override
    public int getItemCount() {
        return visibleNodes.size();
    }

    class FileTreeNodeViewHolder extends RecyclerView.ViewHolder {
        private final View depthIndent;
        private final ImageView ivChevron;
        private final ImageView ivIcon;
        private final TextView tvName;

        FileTreeNodeViewHolder(@NonNull View itemView) {
            super(itemView);
            depthIndent = itemView.findViewById(R.id.view_depth_indent);
            ivChevron = itemView.findViewById(R.id.iv_chevron);
            ivIcon = itemView.findViewById(R.id.iv_node_icon);
            tvName = itemView.findViewById(R.id.tv_node_name);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    FileNode node = visibleNodes.get(pos);
                    if (node.isDirectory()) {
                        listener.onFolderClicked(node);
                    } else {
                        listener.onFileClicked(node.getFile());
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    FileNode node = visibleNodes.get(pos);
                    if (node.isDirectory()) {
                        listener.onFolderLongClicked(node.getFile());
                    } else {
                        listener.onFileLongClicked(node.getFile());
                    }
                    return true;
                }
                return false;
            });
        }

        void bind(FileNode node) {
            // Apply indentation width
            float density = itemView.getContext().getResources().getDisplayMetrics().density;
            int indentPx = (int) (node.getDepth() * 16 * density);
            ViewGroup.LayoutParams lp = depthIndent.getLayoutParams();
            lp.width = indentPx;
            depthIndent.setLayoutParams(lp);

            tvName.setText(node.getName());

            if (node.isDirectory()) {
                ivChevron.setVisibility(View.VISIBLE);
                ivChevron.setImageResource(node.isExpanded() ? R.drawable.ic_chevron_down : R.drawable.ic_chevron_right);
                ivIcon.setImageResource(R.drawable.ic_folder);
            } else {
                ivChevron.setVisibility(View.INVISIBLE);
                String fileName = node.getName().toLowerCase();
                if (fileName.endsWith(".java")) {
                    ivIcon.setImageResource(R.drawable.ic_file_java);
                } else if (fileName.endsWith(".xml")) {
                    ivIcon.setImageResource(R.drawable.ic_file_xml);
                } else {
                    ivIcon.setImageResource(R.drawable.ic_file_code);
                }
            }
        }
    }
}
