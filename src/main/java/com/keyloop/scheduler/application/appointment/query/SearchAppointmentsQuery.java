package com.keyloop.scheduler.application.appointment.query;

import com.keyloop.scheduler.application.cqrs.Query;
import com.keyloop.scheduler.application.dto.AppointmentDto;
import com.keyloop.scheduler.application.dto.PagedResult;

import java.time.LocalDate;

public record SearchAppointmentsQuery(
    Long customerId,
    String status,
    LocalDate dateFrom,
    LocalDate dateTo,
    String sortBy,
    String sortDirection,
    int pageIndex,
    int pageSize
) implements Query<PagedResult<AppointmentDto>> {}
