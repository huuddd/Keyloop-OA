package com.keyloop.scheduler.domain.shared;

public record Error(String code, String message) {
    public static final Error NONE = new Error("", "");
}
