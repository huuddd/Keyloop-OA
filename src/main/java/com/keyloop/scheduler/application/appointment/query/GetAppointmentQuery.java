package com.keyloop.scheduler.application.appointment.query;

import com.keyloop.scheduler.application.cqrs.Query;
import com.keyloop.scheduler.application.dto.AppointmentDto;

import java.util.UUID;

public record GetAppointmentQuery(UUID appointmentId) implements Query<AppointmentDto> {}
