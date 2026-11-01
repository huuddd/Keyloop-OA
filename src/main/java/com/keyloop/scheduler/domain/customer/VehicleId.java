package com.keyloop.scheduler.domain.customer;

import java.util.Objects;

public record VehicleId(Long value) {
    public VehicleId {
        Objects.requireNonNull(value, "VehicleId value must not be null");
        if (value <= 0) throw new IllegalArgumentException("VehicleId must be positive");
    }
    public static VehicleId of(Long value) { return new VehicleId(value); }
    @Override public String toString() { return value.toString(); }
}
