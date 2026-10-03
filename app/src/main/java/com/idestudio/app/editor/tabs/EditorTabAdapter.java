package com.idestudio.app.editor.tabs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.idestudio.app.R;

import java.util.ArrayList;
import java.util.List;

public class EditorTabAdapter extends RecyclerView.Adapter<EditorTabAdapter.TabViewHolder> {

    public interface OnTabInteractionListener {
        void onTabSelected(int index);
        void onTabClosed(int index);
    }

    private final List<OpenFileDocument> openTabs = new ArrayList<>();
    private int activeIndex = 0;
    private final OnTabInteractionListener listener;

    public EditorTabAdapter(OnTabInteractionListener listener) {
        this.listener = listener;
    }

    public void setTabs(List<OpenFileDocument> tabs, int activeIndex) {
        this.openTabs.clear();
        if (tabs != null) {
            this.openTabs.addAll(tabs);
        }
        this.activeIndex = activeIndex;
        notifyDataSetChanged();
    }

    public void setActiveIndex(int activeIndex) {
        int old = this.activeIndex;
        this.activeIndex = activeIndex;
        notifyItemChanged(old);
        notifyItemChanged(activeIndex);
    }

    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_editor_tab, parent, false);
        return new TabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        OpenFileDocument doc = openTabs.get(position);
        holder.bind(doc, position == activeIndex);
    }

    @Override
    public int getItemCount() {
        return openTabs.size();
    }

    class TabViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout containerTab;
        private final TextView tvTitle;
        private final TextView tvDirty;
        private final ImageView btnClose;

        TabViewHolder(@NonNull View itemView) {
            super(itemView);
            containerTab = itemView.findViewById(R.id.container_tab);
            tvTitle = itemView.findViewById(R.id.tv_tab_title);
            tvDirty = itemView.findViewById(R.id.tv_tab_dirty);
            btnClose = itemView.findViewById(R.id.btn_close_tab);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTabSelected(pos);
                }
            });

            btnClose.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTabClosed(pos);
                }
            });
        }

        void bind(OpenFileDocument doc, boolean isActive) {
            tvTitle.setText(doc.getFileName());
            tvDirty.setVisibility(doc.isDirty() ? View.VISIBLE : View.GONE);

            if (isActive) {
                containerTab.setBackgroundResource(R.drawable.bg_tab_active);
                tvTitle.setTextColor(itemView.getContext().getResources().getColor(R.color.primary_indigo));
            } else {
                containerTab.setBackgroundResource(R.drawable.bg_tab_inactive);
                tvTitle.setTextColor(itemView.getContext().getResources().getColor(R.color.text_navy_dark));
            }
        }
    }
}
