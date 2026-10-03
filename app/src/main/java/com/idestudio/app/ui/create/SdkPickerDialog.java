package com.idestudio.app.ui.create;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.idestudio.app.R;
import com.idestudio.app.data.models.SdkInfo;

import java.util.List;

public class SdkPickerDialog {

    public interface SdkSelectionCallback {
        void onSdkChosen(SdkInfo chosenSdk);
    }

    public static void show(Context context, String title, int currentApiLevel, SdkSelectionCallback callback) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sdk_picker, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_sdk_dialog_title);
        RecyclerView recycler = dialogView.findViewById(R.id.recycler_sdks);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel_sdk);

        tvTitle.setText(title);
        recycler.setLayoutManager(new LinearLayoutManager(context));

        List<SdkInfo> list = SdkInfo.getSupportedSdkList();

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .create();

        SdkPickerAdapter adapter = new SdkPickerAdapter(list, currentApiLevel, sdk -> {
            if (callback != null) {
                callback.onSdkChosen(sdk);
            }
            dialog.dismiss();
        });

        recycler.setAdapter(adapter);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
