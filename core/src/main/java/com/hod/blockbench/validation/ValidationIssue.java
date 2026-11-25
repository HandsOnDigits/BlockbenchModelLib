package com.hod.blockbench.validation;

public record ValidationIssue(
        ValidationSeverity severity,
        String path,
        String message
) {
    public ValidationIssue {
        if (severity == null) {
            throw new IllegalArgumentException(
                    "Severity cannot be null."
            );
        }

        if (path == null) {
            throw new IllegalArgumentException(
                    "Path cannot be null."
            );
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Message cannot be blank."
            );
        }
    }
}