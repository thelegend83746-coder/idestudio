package com.idestudio.app.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.idestudio.app.R;
import com.idestudio.app.data.models.ProjectMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    public interface OnProjectInteractionListener {
        void onProjectClick(ProjectMeta project);
        void onProjectLongClick(ProjectMeta project);
    }

    private final List<ProjectMeta> projectList = new ArrayList<>();
    private final OnProjectInteractionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public ProjectAdapter(OnProjectInteractionListener listener) {
        this.listener = listener;
    }

    public void setProjects(List<ProjectMeta> projects) {
        projectList.clear();
        if (projects != null) {
            projectList.addAll(projects);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project_card, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        ProjectMeta project = projectList.get(position);
        holder.bind(project);
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvPackage;
        private final TextView tvSdkTag;
        private final TextView tvLastModified;

        ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_project_name);
            tvPackage = itemView.findViewById(R.id.tv_package_name);
            tvSdkTag = itemView.findViewById(R.id.tv_sdk_tag);
            tvLastModified = itemView.findViewById(R.id.tv_last_modified);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onProjectClick(projectList.get(pos));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onProjectLongClick(projectList.get(pos));
                    return true;
                }
                return false;
            });
        }

        void bind(ProjectMeta project) {
            tvName.setText(project.getName());
            tvPackage.setText(project.getPackageName());
            tvSdkTag.setText("SDK " + project.getMinSdk() + " – " + project.getTargetSdk());
            tvLastModified.setText(dateFormat.format(new Date(project.getLastModified())));
        }
    }
}
