package com.build.studio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlSanitizer {

    private static final Pattern COMPONENT_PATTERN = Pattern.compile("<(activity|service|receiver|provider)([^>]*?)>", Pattern.DOTALL);
    private static final Pattern EXPORTED_PATTERN = Pattern.compile("android:exported\\s*=\\s*\"[^\"]*\"");

    public static String sanitize(String xmlContent, boolean isManifest, int targetSdk) {
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            return xmlContent;
        }

        String result = xmlContent;

        if (isManifest && targetSdk >= 31) {
            result = sanitizeManifestExported(result);
        }

        // Clean trailing null characters or unescaped control chars if any
        result = result.replace("\u0000", "");

        return result;
    }

    public static String sanitizeManifestExported(String manifestContent) {
        // Find each component tag that contains an intent-filter
        // If it doesn't have android:exported, add android:exported="true"
        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;

        // Matches <activity ... > ... </activity> or self-closing
        Pattern componentBlock = Pattern.compile("(<(activity|service|receiver)([^>]*)>)(.*?)(</\\2>)", Pattern.DOTALL);
        Matcher matcher = componentBlock.matcher(manifestContent);

        while (matcher.find()) {
            sb.append(manifestContent, lastIndex, matcher.start());
            String openTag = matcher.group(1);
            String tagName = matcher.group(2);
            String attrs = matcher.group(3);
            String inner = matcher.group(4);
            String closeTag = matcher.group(5);

            if (inner.contains("<intent-filter") && !EXPORTED_PATTERN.matcher(attrs).find()) {
                // inject android:exported="true"
                int tagEnd = openTag.lastIndexOf('>');
                String updatedOpenTag = openTag.substring(0, tagEnd).trim() + " android:exported=\"true\">";
                sb.append(updatedOpenTag).append(inner).append(closeTag);
            } else {
                sb.append(matcher.group(0));
            }
            lastIndex = matcher.end();
        }
        sb.append(manifestContent.substring(lastIndex));
        return sb.toString();
    }

    public static boolean sanitizeFile(File file, int targetSdk) {
        if (file == null || !file.exists() || !file.getName().endsWith(".xml")) {
            return false;
        }
        try {
            byte[] bytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                int offset = 0;
                while (offset < bytes.length) {
                    int read = fis.read(bytes, offset, bytes.length - offset);
                    if (read == -1) break;
                    offset += read;
                }
            }
            String content = new String(bytes, StandardCharsets.UTF_8);
            boolean isManifest = file.getName().equalsIgnoreCase("AndroidManifest.xml");
            String sanitized = sanitize(content, isManifest, targetSdk);

            if (!content.equals(sanitized)) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(sanitized.getBytes(StandardCharsets.UTF_8));
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void sanitizeDirectory(File dir, int targetSdk) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                sanitizeDirectory(f, targetSdk);
            } else if (f.getName().endsWith(".xml")) {
                sanitizeFile(f, targetSdk);
            }
        }
    }
}
