package com.johnvo.retailhub.application.common;

import java.util.Objects;

public record ApplicationError(String code, String message, ErrorType type) {
    public ApplicationError {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Error code is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Error message is required");
        }
        Objects.requireNonNull(type, "Error type is required");
    }
}
