package com.idestudio.app.editor.engine;

public final class JavaCodeFormatter {

    private JavaCodeFormatter() {}

    public static String formatJavaCode(String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            return rawCode;
        }

        String[] lines = rawCode.split("\n", -1);
        StringBuilder formatted = new StringBuilder();
        int indentLevel = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.isEmpty()) {
                formatted.append("\n");
                continue;
            }

            // If line starts with closing brace '}', reduce indent before printing
            if (line.startsWith("}") || line.startsWith(")") || line.startsWith("]")) {
                indentLevel = Math.max(0, indentLevel - 1);
            }

            // Append indentation
            for (int space = 0; space < indentLevel * 4; space++) {
                formatted.append(" ");
            }
            formatted.append(line);
            if (i < lines.length - 1) {
                formatted.append("\n");
            }

            // Count open vs close braces in line
            int openBraces = countOccurrences(line, '{');
            int closeBraces = countOccurrences(line, '}');

            // If line started with '}', we already decremented
            if (line.startsWith("}")) {
                closeBraces--;
            }

            indentLevel += (openBraces - closeBraces);
            if (indentLevel < 0) indentLevel = 0;
        }

        return formatted.toString();
    }

    private static int countOccurrences(String str, char c) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) count++;
        }
        return count;
    }
}
