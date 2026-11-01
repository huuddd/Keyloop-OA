package com.keyloop.scheduler.api.controller;

import com.keyloop.scheduler.application.appointment.query.GetAvailableSlotsQuery;
import com.keyloop.scheduler.application.appointment.query.GetAvailableSlotsQueryHandler;
import com.keyloop.scheduler.application.dto.AvailableSlotDto;
import com.keyloop.scheduler.application.dto.PagedResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dealerships")
public class AvailabilityController {

    private final GetAvailableSlotsQueryHandler availabilityHandler;

    public AvailabilityController(GetAvailableSlotsQueryHandler availabilityHandler) {
        this.availabilityHandler = availabilityHandler;
    }

    @GetMapping("/{dealershipId}/availability")
    public ResponseEntity<PagedResult<AvailableSlotDto>> getAvailability(
            @PathVariable Long dealershipId,
            @RequestParam Long serviceTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {

        var query = new GetAvailableSlotsQuery(
            dealershipId, serviceTypeId, date, pageIndex, pageSize);
        return ResponseEntity.ok(availabilityHandler.handle(query));
    }
}
