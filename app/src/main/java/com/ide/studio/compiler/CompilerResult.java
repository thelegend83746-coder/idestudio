package com.ide.studio.compiler;

/**
 * CompilerResult alias in com.ide.studio.compiler.
 */
public class CompilerResult extends com.tyron.compiler.CompilerResult {
    public CompilerResult(String message) {
        super(message);
    }

    public CompilerResult(boolean isError, String message) {
        super(isError, message);
    }
}
