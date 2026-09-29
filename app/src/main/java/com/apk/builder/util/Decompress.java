package com.apk.builder.util;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Decompress {

    public static boolean unzipFromAssets(Context context, String zipName, String destDirectory) {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        try (InputStream is = context.getAssets().open(zipName);
             ZipInputStream zis = new ZipInputStream(is)) {

            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        int count;
                        while ((count = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, count);
                        }
                    }
                }
                zis.closeEntry();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
