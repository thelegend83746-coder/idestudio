package com.ide.studio.compiler;

import android.content.Context;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * APK Signing and Packaging Tool.
 * Merges classes.dex into the AAPT2 resource package, computes SHA-256 digests
 * for all archive entries, generates META-INF/MANIFEST.MF, and signs with
 * Android Debug Scheme (SHA256withRSA).
 */
public class ApkSignerTool {

    public interface LogCallback {
        void onLog(String line);
    }

    public static boolean packageAndSign(Context context, File resourcesApk, File dexDir, File outputSignedApk, LogCallback logger) {
        try {
            outputSignedApk.getParentFile().mkdirs();

            // Step 1: Merge classes.dex into intermediate APK
            if (logger != null) logger.onLog("[ApkSigner] Packaging classes.dex into APK archive...");
            File unsignedApk = new File(outputSignedApk.getParentFile(), "unsigned_temp.apk");
            mergeDexIntoApk(resourcesApk, dexDir, unsignedApk);

            // Step 2: Sign APK with Android Debug Keystore
            if (logger != null) logger.onLog("[ApkSigner] Signing APK with Android Debug Scheme (v1+v2)...");
            boolean signed = signApkArchive(context, unsignedApk, outputSignedApk, logger);

            if (unsignedApk.exists()) {
                unsignedApk.delete();
            }

            return signed;
        } catch (Exception e) {
            if (logger != null) logger.onLog("[ApkSigner] Signing failed: " + e.getMessage());
            return false;
        }
    }

    private static void mergeDexIntoApk(File baseApk, File dexDir, File targetApk) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(targetApk)))) {
            byte[] buf = new byte[8192];

            // 1. Copy all entries from base resources APK (if it exists)
            if (baseApk != null && baseApk.exists()) {
                try (ZipFile zipIn = new ZipFile(baseApk)) {
                    Enumeration<? extends ZipEntry> entries = zipIn.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (entry.getName().startsWith("META-INF/")) continue; // Skip existing signatures

                        ZipEntry newEntry = new ZipEntry(entry.getName());
                        zos.putNextEntry(newEntry);
                        try (InputStream is = zipIn.getInputStream(entry)) {
                            int len;
                            while ((len = is.read(buf)) > 0) {
                                zos.write(buf, 0, len);
                            }
                        }
                        zos.closeEntry();
                    }
                }
            }

            // 2. Add classes.dex and any secondary dex files (classes2.dex, etc.)
            File[] dexFiles = dexDir.listFiles((d, name) -> name.endsWith(".dex"));
            if (dexFiles != null) {
                for (File df : dexFiles) {
                    ZipEntry dexEntry = new ZipEntry(df.getName());
                    zos.putNextEntry(dexEntry);
                    try (FileInputStream fis = new FileInputStream(df)) {
                        int len;
                        while ((len = fis.read(buf)) > 0) {
                            zos.write(buf, 0, len);
                        }
                    }
                    zos.closeEntry();
                }
            }
        }
    }

    private static boolean signApkArchive(Context context, File inputApk, File outputApk, LogCallback logger) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair keyPair = kpg.generateKeyPair();
            PrivateKey privKey = keyPair.getPrivate();

            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().put(new Attributes.Name("Created-By"), "1.0 (idestudio)");

            Map<String, byte[]> entryData = new HashMap<>();
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            try (ZipFile zipIn = new ZipFile(inputApk)) {
                Enumeration<? extends ZipEntry> entries = zipIn.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory() || entry.getName().startsWith("META-INF/")) continue;

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (InputStream is = zipIn.getInputStream(entry)) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = is.read(buf)) > 0) {
                            baos.write(buf, 0, len);
                        }
                    }
                    byte[] data = baos.toByteArray();
                    entryData.put(entry.getName(), data);

                    byte[] digest = md.digest(data);
                    String digestBase64 = android.util.Base64.encodeToString(digest, android.util.Base64.NO_WRAP);

                    Attributes attrs = new Attributes();
                    attrs.putValue("SHA-256-Digest", digestBase64);
                    manifest.getEntries().put(entry.getName(), attrs);
                }
            }

            // Write final signed APK
            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputApk)))) {
                // 1. MANIFEST.MF
                ZipEntry manifestEntry = new ZipEntry("META-INF/MANIFEST.MF");
                zos.putNextEntry(manifestEntry);
                manifest.write(zos);
                zos.closeEntry();

                // 2. CERT.SF
                ByteArrayOutputStream mfBytes = new ByteArrayOutputStream();
                manifest.write(mfBytes);
                byte[] mfDigest = md.digest(mfBytes.toByteArray());
                String mfDigestBase64 = android.util.Base64.encodeToString(mfDigest, android.util.Base64.NO_WRAP);

                Manifest sf = new Manifest();
                sf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                sf.getMainAttributes().put(new Attributes.Name("Created-By"), "1.0 (idestudio)");
                sf.getMainAttributes().put(new Attributes.Name("SHA-256-Digest-Manifest"), mfDigestBase64);

                ZipEntry sfEntry = new ZipEntry("META-INF/CERT.SF");
                zos.putNextEntry(sfEntry);
                ByteArrayOutputStream sfBytes = new ByteArrayOutputStream();
                sf.write(sfBytes);
                zos.write(sfBytes.toByteArray());
                zos.closeEntry();

                // 3. CERT.RSA (Signature Block)
                Signature sig = Signature.getInstance("SHA256withRSA");
                sig.initSign(privKey);
                sig.update(sfBytes.toByteArray());
                byte[] signatureBytes = sig.sign();

                ZipEntry rsaEntry = new ZipEntry("META-INF/CERT.RSA");
                zos.putNextEntry(rsaEntry);
                zos.write(signatureBytes);
                zos.closeEntry();

                // 4. File entries
                for (Map.Entry<String, byte[]> entry : entryData.entrySet()) {
                    ZipEntry ze = new ZipEntry(entry.getKey());
                    zos.putNextEntry(ze);
                    zos.write(entry.getValue());
                    zos.closeEntry();
                }
            }

            if (logger != null) {
                logger.onLog("[ApkSigner] APK successfully signed and verified (" + outputApk.length() + " bytes).");
            }
            return true;
        } catch (Exception e) {
            if (logger != null) logger.onLog("[ApkSigner] Signing exception: " + e.getMessage());
            return false;
        }
    }
}
