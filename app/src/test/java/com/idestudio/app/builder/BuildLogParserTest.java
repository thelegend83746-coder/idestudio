package com.idestudio.app.builder;

import com.idestudio.app.builder.parser.BuildError;
import com.idestudio.app.builder.parser.BuildLogParser;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BuildLogParserTest {

    @Test
    public void testParseStandardJavacError() {
        String log = "[JAVAC] Compiling...\n"
                + "app/src/main/java/com/demo/MainActivity.java:15: error: ';' expected\n"
                + "        int x = 10\n"
                + "                  ^\n";

        List<BuildError> errors = BuildLogParser.parseLog(log);
        assertFalse(errors.isEmpty());

        BuildError err = errors.get(0);
        assertEquals("MainActivity.java", err.getFileName());
        assertEquals(15, err.getLineNumber());
        assertEquals("';' expected", err.getErrorMessage());
    }

    @Test
    public void testParseEcjError() {
        String log = "1. ERROR in app/src/main/java/com/demo/MainActivity.java (at line 22)\n"
                + "    cannot find symbol: variable foo\n";

        List<BuildError> errors = BuildLogParser.parseLog(log);
        assertFalse(errors.isEmpty());

        BuildError err = errors.get(0);
        assertEquals("MainActivity.java", err.getFileName());
        assertEquals(22, err.getLineNumber());
    }
}
