package com.keyloop.scheduler.domain.dealership;

import java.util.Objects;

public record TechnicianId(Long value) {
    public TechnicianId {
        Objects.requireNonNull(value, "TechnicianId value must not be null");
        if (value <= 0) throw new IllegalArgumentException("TechnicianId must be positive");
    }
    public static TechnicianId of(Long value) { return new TechnicianId(value); }
    @Override public String toString() { return value.toString(); }
}
