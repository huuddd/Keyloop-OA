package com.keyloop.scheduler.application.dto;

import com.keyloop.scheduler.domain.appointment.Appointment;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentDto(
    UUID id,
    Long customerId,
    Long vehicleId,
    Long serviceBayId,
    Long technicianId,
    Long serviceTypeId,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AppointmentDto fromDomain(Appointment appointment) {
        return new AppointmentDto(
            appointment.getId().value(),
            appointment.getCustomerId().value(),
            appointment.getVehicleId().value(),
            appointment.getServiceBayId().value(),
            appointment.getTechnicianId().value(),
            appointment.getServiceTypeId().value(),
            appointment.getStartTime(),
            appointment.getEndTime(),
            appointment.getStatus().name(),
            appointment.getCreatedAt(),
            appointment.getUpdatedAt());
    }
}
