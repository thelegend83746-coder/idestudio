package com.idestudio.app.editor.engine;

import android.graphics.Color;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance syntax highlighter supporting Java, XML, Gradle, and JSON.
 * Uses modern VS Code / IntelliJ dark theme color scheme.
 */
public final class JavaSyntaxHighlighter {

    // Java Patterns
    private static final Pattern JAVA_KEYWORDS = Pattern.compile(
            "\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|"
            + "default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|"
            + "import|instanceof|int|interface|long|native|new|package|private|protected|"
            + "public|return|short|static|strictfp|super|switch|synchronized|this|throw|"
            + "throws|transient|try|void|volatile|while|true|false|null|var|record|yield)\\b"
    );

    private static final Pattern JAVA_STRINGS = Pattern.compile("\"(\\\\.|[^\"])*\"");
    private static final Pattern JAVA_CHARS = Pattern.compile("'(\\\\.|[^'])*'");
    private static final Pattern JAVA_NUMBERS = Pattern.compile("\\b(\\d+(\\.\\d+)?([eE][+-]?\\d+)?[fFdDlL]?|0[xX][0-9a-fA-F]+)\\b");
    private static final Pattern JAVA_ANNOTATIONS = Pattern.compile("@[A-Za-z0-9_]+");
    private static final Pattern JAVA_LINE_COMMENTS = Pattern.compile("//.*");
    private static final Pattern JAVA_BLOCK_COMMENTS = Pattern.compile("/\\*[\\s\\S]*?\\*/");

    // XML Patterns
    private static final Pattern XML_TAGS = Pattern.compile("</?[a-zA-Z0-9_.:-]+");
    private static final Pattern XML_ATTRIBUTES = Pattern.compile("[a-zA-Z0-9_.:-]+(?=\\=)");
    private static final Pattern XML_COMMENTS = Pattern.compile("<!--[\\s\\S]*?-->");

    // Colors (VS Code Dark Palette)
    private static final int COLOR_KEYWORD = Color.parseColor("#569CD6");     // Blue
    private static final int COLOR_STRING = Color.parseColor("#CE9178");      // Warm Amber / Peach
    private static final int COLOR_NUMBER = Color.parseColor("#B5CEA8");      // Light Green
    private static final int COLOR_ANNOTATION = Color.parseColor("#DCDCAA");  // Soft Yellow
    private static final int COLOR_COMMENT = Color.parseColor("#6A9955");     // Muted Green
    private static final int COLOR_XML_TAG = Color.parseColor("#569CD6");     // Blue Tag
    private static final int COLOR_XML_ATTR = Color.parseColor("#9CDCFE");    // Light Cyan Attr

    public static void highlight(Editable editable, String extension) {
        if (editable == null || editable.length() == 0) return;

        // Strip previous syntax spans
        ForegroundColorSpan[] existingSpans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : existingSpans) {
            editable.removeSpan(span);
        }

        String ext = extension != null ? extension.toLowerCase() : "java";

        if (ext.equals("xml")) {
            highlightXml(editable);
        } else {
            highlightJava(editable);
        }
    }

    private static void highlightJava(Editable editable) {
        String text = editable.toString();

        // 1. Numbers
        applyPattern(editable, JAVA_NUMBERS.matcher(text), COLOR_NUMBER);

        // 2. Keywords
        applyPattern(editable, JAVA_KEYWORDS.matcher(text), COLOR_KEYWORD);

        // 3. Annotations
        applyPattern(editable, JAVA_ANNOTATIONS.matcher(text), COLOR_ANNOTATION);

        // 4. Strings & Chars
        applyPattern(editable, JAVA_STRINGS.matcher(text), COLOR_STRING);
        applyPattern(editable, JAVA_CHARS.matcher(text), COLOR_STRING);

        // 5. Comments (highest priority to override any strings inside comments)
        applyPattern(editable, JAVA_LINE_COMMENTS.matcher(text), COLOR_COMMENT);
        applyPattern(editable, JAVA_BLOCK_COMMENTS.matcher(text), COLOR_COMMENT);
    }

    private static void highlightXml(Editable editable) {
        String text = editable.toString();

        // 1. XML Tags
        applyPattern(editable, XML_TAGS.matcher(text), COLOR_XML_TAG);

        // 2. XML Attributes
        applyPattern(editable, XML_ATTRIBUTES.matcher(text), COLOR_XML_ATTR);

        // 3. Strings / Values
        applyPattern(editable, JAVA_STRINGS.matcher(text), COLOR_STRING);

        // 4. XML Comments
        applyPattern(editable, XML_COMMENTS.matcher(text), COLOR_COMMENT);
    }

    private static void applyPattern(Editable editable, Matcher matcher, int color) {
        while (matcher.find()) {
            editable.setSpan(
                    new ForegroundColorSpan(color),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
}
