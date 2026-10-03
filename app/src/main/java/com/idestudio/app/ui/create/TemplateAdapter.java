package com.idestudio.app.ui.create;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.idestudio.app.R;
import com.idestudio.app.data.models.TemplateModel;

import java.util.List;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder> {

    public interface OnTemplateSelectedListener {
        void onTemplateSelected(TemplateModel template);
    }

    private final List<TemplateModel> templates;
    private final OnTemplateSelectedListener listener;
    private int selectedPosition = 0;

    public TemplateAdapter(List<TemplateModel> templates, OnTemplateSelectedListener listener) {
        this.templates = templates;
        this.listener = listener;
        for (int i = 0; i < templates.size(); i++) {
            if (templates.get(i).isSelected()) {
                selectedPosition = i;
                break;
            }
        }
    }

    public TemplateModel getSelectedTemplate() {
        if (selectedPosition >= 0 && selectedPosition < templates.size()) {
            return templates.get(selectedPosition);
        }
        return templates.get(0);
    }

    @NonNull
    @Override
    public TemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_template_card, parent, false);
        return new TemplateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        TemplateModel item = templates.get(position);
        holder.bind(item, position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    class TemplateViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout container;
        private final TextView tvTitle;
        private final TextView tvDescription;
        private final ImageView ivCheck;

        TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.container_template_item);
            tvTitle = itemView.findViewById(R.id.tv_template_title);
            tvDescription = itemView.findViewById(R.id.tv_template_description);
            ivCheck = itemView.findViewById(R.id.iv_template_check);

            itemView.setOnClickListener(v -> {
                int previousPos = selectedPosition;
                selectedPosition = getAdapterPosition();
                if (selectedPosition != RecyclerView.NO_POSITION) {
                    for (int i = 0; i < templates.size(); i++) {
                        templates.get(i).setSelected(i == selectedPosition);
                    }
                    notifyItemChanged(previousPos);
                    notifyItemChanged(selectedPosition);
                    if (listener != null) {
                        listener.onTemplateSelected(templates.get(selectedPosition));
                    }
                }
            });
        }

        void bind(TemplateModel item, boolean isSelected) {
            tvTitle.setText(item.getName());
            tvDescription.setText(item.getDescription());

            if (isSelected) {
                container.setBackgroundResource(R.drawable.bg_template_selected);
                ivCheck.setVisibility(View.VISIBLE);
            } else {
                container.setBackgroundResource(R.drawable.bg_template_unselected);
                ivCheck.setVisibility(View.GONE);
            }
        }
    }
}
