package com.keyloop.scheduler.domain.appointment.event;

import com.keyloop.scheduler.domain.shared.DomainEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentCheckedInEvent(
    UUID eventId, Instant occurredOn,
    UUID appointmentId, LocalDateTime checkedInAt
) implements DomainEvent {

    public static AppointmentCheckedInEvent of(
            UUID appointmentId, LocalDateTime checkedInAt) {
        return new AppointmentCheckedInEvent(
            UUID.randomUUID(), Instant.now(), appointmentId, checkedInAt);
    }
}
