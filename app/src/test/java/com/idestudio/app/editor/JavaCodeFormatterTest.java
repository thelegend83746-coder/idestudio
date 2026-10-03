package com.idestudio.app.editor;

import com.idestudio.app.editor.engine.JavaCodeFormatter;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class JavaCodeFormatterTest {

    @Test
    public void testFormatJavaCodeIndentation() {
        String unformatted = "public class Test {\nint a = 1;\nvoid run() {\nSystem.out.println(a);\n}\n}";
        String formatted = JavaCodeFormatter.formatJavaCode(unformatted);

        assertTrue(formatted.contains("    int a = 1;"));
        assertTrue(formatted.contains("        System.out.println(a);"));
        assertTrue(formatted.endsWith("}"));
    }
}
