package com.keyloop.scheduler.domain.customer;

import java.util.Objects;

public record CustomerId(Long value) {
    public CustomerId {
        Objects.requireNonNull(value, "CustomerId value must not be null");
        if (value <= 0) throw new IllegalArgumentException("CustomerId must be positive");
    }
    public static CustomerId of(Long value) { return new CustomerId(value); }
    @Override public String toString() { return value.toString(); }
}
