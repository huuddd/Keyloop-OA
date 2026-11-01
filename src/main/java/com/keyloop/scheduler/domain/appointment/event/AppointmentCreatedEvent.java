package com.keyloop.scheduler.domain.appointment.event;

import com.keyloop.scheduler.domain.shared.DomainEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentCreatedEvent(
    UUID eventId, Instant occurredOn,
    UUID appointmentId, Long customerId, Long serviceBayId,
    Long technicianId, LocalDateTime startTime, LocalDateTime endTime
) implements DomainEvent {

    public static AppointmentCreatedEvent of(
            UUID appointmentId, Long customerId, Long serviceBayId,
            Long technicianId, LocalDateTime startTime, LocalDateTime endTime) {
        return new AppointmentCreatedEvent(
            UUID.randomUUID(), Instant.now(),
            appointmentId, customerId, serviceBayId,
            technicianId, startTime, endTime);
    }
}
