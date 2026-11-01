package com.keyloop.scheduler.domain.servicetype;

import java.util.Objects;

public record ServiceTypeId(Long value) {
    public ServiceTypeId {
        Objects.requireNonNull(value, "ServiceTypeId value must not be null");
        if (value <= 0) throw new IllegalArgumentException("ServiceTypeId must be positive");
    }
    public static ServiceTypeId of(Long value) { return new ServiceTypeId(value); }
    @Override public String toString() { return value.toString(); }
}
