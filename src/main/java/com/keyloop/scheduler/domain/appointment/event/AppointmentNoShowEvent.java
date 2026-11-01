package com.keyloop.scheduler.domain.appointment.event;

import com.keyloop.scheduler.domain.shared.DomainEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentNoShowEvent(
    UUID eventId, Instant occurredOn,
    UUID appointmentId, Long customerId, LocalDateTime markedAt
) implements DomainEvent {

    public static AppointmentNoShowEvent of(
            UUID appointmentId, Long customerId, LocalDateTime markedAt) {
        return new AppointmentNoShowEvent(
            UUID.randomUUID(), Instant.now(), appointmentId, customerId, markedAt);
    }
}
