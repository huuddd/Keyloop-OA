package com.keyloop.scheduler.api.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateAppointmentRequest(
    @NotNull Long customerId,
    @NotNull Long vehicleId,
    @NotNull Long dealershipId,
    @NotNull Long serviceTypeId,
    @NotNull @Future LocalDateTime startTime
) {}
