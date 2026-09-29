package com.tyron.compiler;

public class CompilerResult {
    private final boolean isError;
    private final String message;

    public CompilerResult(String message) {
        this(false, message);
    }

    public CompilerResult(boolean isError, String message) {
        this.isError = isError;
        this.message = message;
    }

    public boolean isError() {
        return isError;
    }

    public String getMessage() {
        return message;
    }
}
