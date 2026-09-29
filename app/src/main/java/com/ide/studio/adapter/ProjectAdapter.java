package com.ide.studio.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ide.studio.R;
import com.ide.studio.model.Project;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying the list of created projects on the home screen.
 */
public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
        void onProjectLongClick(Project project);
    }

    private final Context mContext;
    private final List<Project> mProjects = new ArrayList<>();
    private final OnProjectClickListener mListener;

    public ProjectAdapter(Context context, OnProjectClickListener listener) {
        this.mContext = context;
        this.mListener = listener;
    }

    public void setProjects(List<Project> projects) {
        mProjects.clear();
        if (projects != null) {
            mProjects.addAll(projects);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = mProjects.get(position);
        holder.tvName.setText(project.getName());
        holder.tvPath.setText(project.getRootDirectory().getAbsolutePath());

        // Check if project has a custom icon in res/drawable or root
        File iconFile = new File(project.getRootDirectory(), "app/src/main/res/drawable/ic_launcher.png");
        if (iconFile.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(iconFile.getAbsolutePath());
            if (bmp != null) {
                holder.ivLogo.setImageBitmap(bmp);
            } else {
                holder.ivLogo.setImageResource(R.drawable.ic_idestudio_logo);
            }
        } else {
            holder.ivLogo.setImageResource(R.drawable.ic_idestudio_logo);
        }

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onProjectClick(project);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (mListener != null) {
                mListener.onProjectLongClick(project);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return mProjects.size();
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        ImageView ivLogo;
        TextView tvName;
        TextView tvPath;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            ivLogo = itemView.findViewById(R.id.iv_project_logo);
            tvName = itemView.findViewById(R.id.tv_project_name);
            tvPath = itemView.findViewById(R.id.tv_project_path);
        }
    }
}
