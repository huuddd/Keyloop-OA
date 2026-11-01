package com.keyloop.scheduler.domain.appointment.event;

import com.keyloop.scheduler.domain.shared.DomainEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentCompletedEvent(
    UUID eventId, Instant occurredOn,
    UUID appointmentId, LocalDateTime completedAt
) implements DomainEvent {

    public static AppointmentCompletedEvent of(
            UUID appointmentId, LocalDateTime completedAt) {
        return new AppointmentCompletedEvent(
            UUID.randomUUID(), Instant.now(), appointmentId, completedAt);
    }
}
