package com.keyloop.scheduler.application.appointment.query;

import com.keyloop.scheduler.application.cqrs.Query;
import com.keyloop.scheduler.application.dto.AvailableSlotDto;
import com.keyloop.scheduler.application.dto.PagedResult;

import java.time.LocalDate;

public record GetAvailableSlotsQuery(
    Long dealershipId,
    Long serviceTypeId,
    LocalDate date,
    int pageIndex,
    int pageSize
) implements Query<PagedResult<AvailableSlotDto>> {}
