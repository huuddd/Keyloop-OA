package com.keyloop.scheduler.domain.dealership;

import java.util.Objects;

public record DealershipId(Long value) {
    public DealershipId {
        Objects.requireNonNull(value, "DealershipId value must not be null");
        if (value <= 0) throw new IllegalArgumentException("DealershipId must be positive");
    }
    public static DealershipId of(Long value) { return new DealershipId(value); }
    @Override public String toString() { return value.toString(); }
}
