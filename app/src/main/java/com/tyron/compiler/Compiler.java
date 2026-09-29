package com.tyron.compiler;

import java.io.File;

public abstract class Compiler {

    public interface OnProgressUpdateListener {
        void onProgressUpdate(String... values);
    }

    protected OnProgressUpdateListener listener;

    public void setProgressListener(OnProgressUpdateListener listener) {
        this.listener = listener;
    }

    public void onProgressUpdate(String... values) {
        if (listener != null) {
            listener.onProgressUpdate(values);
        }
    }

    public abstract void prepare() throws Exception;

    public abstract void run() throws Exception;

    public abstract File getAndroidJarFile();
}
