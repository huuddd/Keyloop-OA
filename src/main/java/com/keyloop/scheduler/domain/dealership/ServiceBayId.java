package com.keyloop.scheduler.domain.dealership;

import java.util.Objects;

public record ServiceBayId(Long value) {
    public ServiceBayId {
        Objects.requireNonNull(value, "ServiceBayId value must not be null");
        if (value <= 0) throw new IllegalArgumentException("ServiceBayId must be positive");
    }
    public static ServiceBayId of(Long value) { return new ServiceBayId(value); }
    @Override public String toString() { return value.toString(); }
}
