package com.keyloop.scheduler.domain.shared;

import java.util.Objects;
import java.util.function.Function;

public sealed interface Result<T> {

    boolean isSuccess();
    boolean isFailure();
    T getValue();
    Error getError();

    <R> R fold(Function<T, R> onSuccess, Function<Error, R> onFailure);

    static <T> Result<T> success(T value) { return new Success<>(value); }
    static <T> Result<T> failure(Error error) { return new Failure<>(error); }
    static Result<Void> success() { return new Success<>(null); }

    record Success<T>(T value) implements Result<T> {
        @Override public boolean isSuccess() { return true; }
        @Override public boolean isFailure() { return false; }
        @Override public T getValue() { return value; }
        @Override public Error getError() {
            throw new IllegalStateException("No error on success result");
        }
        @Override
        public <R> R fold(Function<T, R> onSuccess, Function<Error, R> onFailure) {
            return onSuccess.apply(value);
        }
    }

    record Failure<T>(Error error) implements Result<T> {
        public Failure { Objects.requireNonNull(error, "Error must not be null"); }
        @Override public boolean isSuccess() { return false; }
        @Override public boolean isFailure() { return true; }
        @Override public T getValue() {
            throw new IllegalStateException("No value on failure result: " + error);
        }
        @Override public Error getError() { return error; }
        @Override
        public <R> R fold(Function<T, R> onSuccess, Function<Error, R> onFailure) {
            return onFailure.apply(error);
        }
    }
}
