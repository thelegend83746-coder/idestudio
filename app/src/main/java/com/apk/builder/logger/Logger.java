package com.apk.builder.logger;

import android.util.Log;

public class Logger {
    private LogListener mListener;

    public interface LogListener {
        void onLog(String tag, String message);
    }

    public void setLogListener(LogListener listener) {
        this.mListener = listener;
    }

    public void d(String tag, String message) {
        Log.d(tag, message);
        if (mListener != null) {
            mListener.onLog(tag, message);
        }
    }

    public void w(String tag, String message) {
        Log.w(tag, message);
        if (mListener != null) {
            mListener.onLog(tag, message);
        }
    }

    public void e(String tag, String message) {
        Log.e(tag, message);
        if (mListener != null) {
            mListener.onLog(tag, message);
        }
    }
}
