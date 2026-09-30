package com.ide.studio.view;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.ide.studio.R;

public class BuildLogDialog {
    public interface OnFixWithAiListener {
        void onFixWithAi(String errorLogs);
    }

    private Dialog dialog;
    private TextView tvLogs;
    private ScrollView scrollView;
    private Button btnFixWithAi;
    private StringBuilder logBuffer = new StringBuilder();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Context context;
    private OnFixWithAiListener fixListener;

    public BuildLogDialog(Context context) {
        this.context = context;
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_build_log);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.90),
                    (int) (context.getResources().getDisplayMetrics().heightPixels * 0.65)
            );
        }

        tvLogs = dialog.findViewById(R.id.tv_logs);
        scrollView = dialog.findViewById(R.id.scroll_logs);
        btnFixWithAi = dialog.findViewById(R.id.btn_fix_with_ai);

        Button btnCopy = dialog.findViewById(R.id.btn_copy_logs);
        Button btnDone = dialog.findViewById(R.id.btn_done);

        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Build Logs", logBuffer.toString());
            if (cm != null) {
                cm.setPrimaryClip(clip);
                Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        btnFixWithAi.setOnClickListener(v -> {
            dialog.dismiss();
            if (fixListener != null) {
                fixListener.onFixWithAi(logBuffer.toString());
            }
        });

        btnDone.setOnClickListener(v -> dialog.dismiss());
    }

    public void setOnFixWithAiListener(OnFixWithAiListener listener) {
        this.fixListener = listener;
    }

    public void setFixWithAiVisible(boolean visible) {
        mainHandler.post(() -> {
            if (btnFixWithAi != null) {
                btnFixWithAi.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        });
    }

    public void show() {
        logBuffer.setLength(0);
        if (tvLogs != null) tvLogs.setText("");
        setFixWithAiVisible(false);
        dialog.show();
    }

    public void appendLog(String line) {
        logBuffer.append(line).append("\n");
        mainHandler.post(() -> {
            if (tvLogs != null) {
                tvLogs.setText(logBuffer.toString());
            }
            if (scrollView != null) {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    public String getLogs() {
        return logBuffer.toString();
    }

    public void dismiss() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
