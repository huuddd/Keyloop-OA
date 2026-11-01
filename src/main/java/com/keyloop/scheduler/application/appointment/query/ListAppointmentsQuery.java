package com.keyloop.scheduler.application.appointment.query;

import com.keyloop.scheduler.application.cqrs.Query;
import com.keyloop.scheduler.application.dto.AppointmentDto;

import java.util.List;

public record ListAppointmentsQuery(Long customerId) implements Query<List<AppointmentDto>> {}
