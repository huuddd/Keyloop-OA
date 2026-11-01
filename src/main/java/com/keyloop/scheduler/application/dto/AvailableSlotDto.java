package com.keyloop.scheduler.application.dto;

import java.time.LocalDateTime;

public record AvailableSlotDto(
    LocalDateTime startTime,
    LocalDateTime endTime,
    Long serviceBayId,
    String serviceBayName,
    Long technicianId,
    String technicianName
) {}
