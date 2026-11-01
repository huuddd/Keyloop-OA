package com.keyloop.scheduler.domain.appointment.event;

import com.keyloop.scheduler.domain.shared.DomainEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentCancelledEvent(
    UUID eventId, Instant occurredOn,
    UUID appointmentId, Long customerId, LocalDateTime cancelledAt
) implements DomainEvent {

    public static AppointmentCancelledEvent of(
            UUID appointmentId, Long customerId, LocalDateTime cancelledAt) {
        return new AppointmentCancelledEvent(
            UUID.randomUUID(), Instant.now(), appointmentId, customerId, cancelledAt);
    }
}
