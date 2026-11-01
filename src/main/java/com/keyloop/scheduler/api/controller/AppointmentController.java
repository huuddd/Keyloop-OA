package com.keyloop.scheduler.api.controller;

import com.keyloop.scheduler.api.exception.GlobalExceptionHandler;
import com.keyloop.scheduler.api.request.CreateAppointmentRequest;
import com.keyloop.scheduler.application.appointment.command.*;
import com.keyloop.scheduler.application.appointment.query.GetAppointmentQuery;
import com.keyloop.scheduler.application.appointment.query.GetAppointmentQueryHandler;
import com.keyloop.scheduler.application.appointment.query.SearchAppointmentsQuery;
import com.keyloop.scheduler.application.appointment.query.SearchAppointmentsQueryHandler;
import com.keyloop.scheduler.application.dto.AppointmentDto;
import com.keyloop.scheduler.application.dto.PagedResult;
import com.keyloop.scheduler.domain.shared.Error;
import com.keyloop.scheduler.domain.shared.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final CreateAppointmentCommandHandler createHandler;
    private final CancelAppointmentCommandHandler cancelHandler;
    private final CheckInAppointmentCommandHandler checkInHandler;
    private final CompleteAppointmentCommandHandler completeHandler;
    private final MarkNoShowCommandHandler noShowHandler;
    private final GetAppointmentQueryHandler getHandler;
    private final SearchAppointmentsQueryHandler searchHandler;

    public AppointmentController(
            CreateAppointmentCommandHandler createHandler,
            CancelAppointmentCommandHandler cancelHandler,
            CheckInAppointmentCommandHandler checkInHandler,
            CompleteAppointmentCommandHandler completeHandler,
            MarkNoShowCommandHandler noShowHandler,
            GetAppointmentQueryHandler getHandler,
            SearchAppointmentsQueryHandler searchHandler) {
        this.createHandler = createHandler;
        this.cancelHandler = cancelHandler;
        this.checkInHandler = checkInHandler;
        this.completeHandler = completeHandler;
        this.noShowHandler = noShowHandler;
        this.getHandler = getHandler;
        this.searchHandler = searchHandler;
    }

    @PostMapping
    public ResponseEntity<?> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request,
            HttpServletRequest httpRequest) {

        var command = new CreateAppointmentCommand(
            request.customerId(), request.vehicleId(),
            request.dealershipId(), request.serviceTypeId(),
            request.startTime());

        var result = createHandler.handle(command);
        return toResponse(result, HttpStatus.CREATED, httpRequest);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDto> getAppointment(@PathVariable UUID id) {
        return ResponseEntity.ok(getHandler.handle(new GetAppointmentQuery(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelAppointment(@PathVariable UUID id,
                                                HttpServletRequest httpRequest) {
        var result = cancelHandler.handle(new CancelAppointmentCommand(id));
        return toVoidResponse(result, httpRequest);
    }

    @PatchMapping("/{id}/check-in")
    public ResponseEntity<?> checkInAppointment(@PathVariable UUID id,
                                                 HttpServletRequest httpRequest) {
        var result = checkInHandler.handle(new CheckInAppointmentCommand(id));
        return toVoidResponse(result, httpRequest);
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<?> completeAppointment(@PathVariable UUID id,
                                                  HttpServletRequest httpRequest) {
        var result = completeHandler.handle(new CompleteAppointmentCommand(id));
        return toVoidResponse(result, httpRequest);
    }

    @PatchMapping("/{id}/no-show")
    public ResponseEntity<?> markNoShowAppointment(@PathVariable UUID id,
                                                    HttpServletRequest httpRequest) {
        var result = noShowHandler.handle(new MarkNoShowCommand(id));
        return toVoidResponse(result, httpRequest);
    }

    @GetMapping
    public ResponseEntity<PagedResult<AppointmentDto>> listAppointments(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(defaultValue = "startTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        var query = new SearchAppointmentsQuery(
            customerId, status, dateFrom, dateTo,
            sortBy, sortDirection, pageIndex, pageSize);
        return ResponseEntity.ok(searchHandler.handle(query));
    }

    // --- Private helpers ---

    private <T> ResponseEntity<?> toResponse(Result<T> result,
                                              HttpStatus successStatus,
                                              HttpServletRequest httpRequest) {
        return result.fold(
            value -> ResponseEntity.status(successStatus).body(value),
            error -> {
                var status = GlobalExceptionHandler.mapErrorToStatus(error);
                var problem = GlobalExceptionHandler.fromDomainError(
                    error, status, httpRequest.getRequestURI());
                return ResponseEntity.status(status).body(problem);
            }
        );
    }

    private ResponseEntity<?> toVoidResponse(Result<Void> result,
                                              HttpServletRequest httpRequest) {
        return result.fold(
            v -> ResponseEntity.noContent().build(),
            error -> {
                var status = GlobalExceptionHandler.mapErrorToStatus(error);
                var problem = GlobalExceptionHandler.fromDomainError(
                    error, status, httpRequest.getRequestURI());
                return ResponseEntity.status(status).body(problem);
            }
        );
    }
}
