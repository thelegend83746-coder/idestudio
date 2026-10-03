package com.idestudio.app.ui.create;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.idestudio.app.R;
import com.idestudio.app.data.models.SdkInfo;

import java.util.List;

public class SdkPickerAdapter extends RecyclerView.Adapter<SdkPickerAdapter.SdkViewHolder> {

    public interface OnSdkSelectedListener {
        void onSdkSelected(SdkInfo sdk);
    }

    private final List<SdkInfo> sdkList;
    private final OnSdkSelectedListener listener;
    private int selectedApiLevel;

    public SdkPickerAdapter(List<SdkInfo> sdkList, int initialApiLevel, OnSdkSelectedListener listener) {
        this.sdkList = sdkList;
        this.selectedApiLevel = initialApiLevel;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SdkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sdk_picker, parent, false);
        return new SdkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SdkViewHolder holder, int position) {
        SdkInfo item = sdkList.get(position);
        holder.bind(item, item.getApiLevel() == selectedApiLevel);
    }

    @Override
    public int getItemCount() {
        return sdkList.size();
    }

    class SdkViewHolder extends RecyclerView.ViewHolder {
        private final RadioButton rbSelected;
        private final TextView tvName;
        private final TextView tvVersion;

        SdkViewHolder(@NonNull View itemView) {
            super(itemView);
            rbSelected = itemView.findViewById(R.id.rb_sdk_selected);
            tvName = itemView.findViewById(R.id.tv_sdk_name);
            tvVersion = itemView.findViewById(R.id.tv_sdk_version);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    selectedApiLevel = sdkList.get(pos).getApiLevel();
                    notifyDataSetChanged();
                    if (listener != null) {
                        listener.onSdkSelected(sdkList.get(pos));
                    }
                }
            });
        }

        void bind(SdkInfo sdk, boolean isSelected) {
            rbSelected.setChecked(isSelected);
            tvName.setText("API " + sdk.getApiLevel() + " — " + sdk.getCodeName());
            tvVersion.setText(sdk.getVersionName());
        }
    }
}
