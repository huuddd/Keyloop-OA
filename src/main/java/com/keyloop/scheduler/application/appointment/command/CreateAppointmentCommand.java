package com.keyloop.scheduler.application.appointment.command;

import com.keyloop.scheduler.application.cqrs.Command;
import com.keyloop.scheduler.application.dto.AppointmentDto;

import java.time.LocalDateTime;

public record CreateAppointmentCommand(
    Long customerId,
    Long vehicleId,
    Long dealershipId,
    Long serviceTypeId,
    LocalDateTime startTime
) implements Command<AppointmentDto> {}
