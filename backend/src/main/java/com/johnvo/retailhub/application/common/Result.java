package com.johnvo.retailhub.application.common;

import java.util.Objects;

public final class Result<T> {
    private final T value;
    private final ApplicationError error;

    private Result(T value, ApplicationError error) {
        this.value = value;
        this.error = error;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }

    public static Result<Void> success() {
        return new Result<>(null, null);
    }

    public static <T> Result<T> failure(ApplicationError error) {
        return new Result<>(null, Objects.requireNonNull(error));
    }

    public boolean isSuccess() {
        return error == null;
    }

    public boolean isFailure() {
        return error != null;
    }

    public T value() {
        if (isFailure()) {
            throw new IllegalStateException("A failed result has no value");
        }
        return value;
    }

    public ApplicationError error() {
        if (isSuccess()) {
            throw new IllegalStateException("A successful result has no error");
        }
        return error;
    }
}
