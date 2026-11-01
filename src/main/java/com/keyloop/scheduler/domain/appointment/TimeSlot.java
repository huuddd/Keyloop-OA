package com.keyloop.scheduler.domain.appointment;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public record TimeSlot(LocalDateTime startTime, LocalDateTime endTime) {

    public TimeSlot {
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
    }

    public boolean overlapsWith(TimeSlot other) {
        return this.startTime.isBefore(other.endTime)
            && this.endTime.isAfter(other.startTime);
    }

    public static TimeSlot of(LocalDateTime startTime, int durationMinutes) {
        return new TimeSlot(startTime, startTime.plusMinutes(durationMinutes));
    }

    public int durationMinutes() {
        return (int) Duration.between(startTime, endTime).toMinutes();
    }
}
