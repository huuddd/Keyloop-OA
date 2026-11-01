package com.keyloop.scheduler.domain.appointment;

import java.util.Objects;
import java.util.UUID;

public record AppointmentId(UUID value) {
    public AppointmentId {
        Objects.requireNonNull(value, "AppointmentId value must not be null");
    }
    public static AppointmentId generate() { return new AppointmentId(UUID.randomUUID()); }
    public static AppointmentId of(UUID value) { return new AppointmentId(value); }
    @Override public String toString() { return value.toString(); }
}
