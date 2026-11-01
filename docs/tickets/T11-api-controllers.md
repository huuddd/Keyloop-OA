# T11 — API: REST Controllers + Exception Handler + Filters

**Type:** Feature | **Priority:** P1
**Depends on:** T09, T10
**Blocks:** T12

---

## Description

Create REST controllers that delegate to CQRS handlers, a global exception handler producing RFC 7807 ProblemDetail responses, and cross-cutting filters for correlation ID tracking and idempotency.

Key design decisions:
- Controllers receive `Result<T>` from command handlers and map success/failure to appropriate HTTP responses
- `GlobalExceptionHandler` extends `ResponseEntityExceptionHandler` and produces RFC 7807 `ProblemDetail` with extension properties (`correlationId`, `timestamp`, `errorCode`)
- `CorrelationIdFilter` — injects/generates `X-Correlation-ID` into MDC for structured logging
- `IdempotencyFilter` — caches POST responses by `Idempotency-Key` header (24h TTL)

## Acceptance Criteria

- [ ] `POST /api/v1/appointments` → create appointment (201) with `Idempotency-Key` header
- [ ] `GET /api/v1/appointments/{id}` → appointment details
- [ ] `DELETE /api/v1/appointments/{id}` → cancel (204)
- [ ] `PATCH /api/v1/appointments/{id}/check-in` → check in (204)
- [ ] `PATCH /api/v1/appointments/{id}/complete` → complete (204)
- [ ] `PATCH /api/v1/appointments/{id}/no-show` → mark no-show (204)
- [ ] `GET /api/v1/appointments?customerId=&status=&pageIndex=0&pageSize=20&sortBy=startTime&sortDirection=desc` → paginated search
- [ ] `GET /api/v1/dealerships/{id}/availability?serviceTypeId=&date=&pageIndex=0&pageSize=20` → available slots
- [ ] `GlobalExceptionHandler` → RFC 7807 ProblemDetail for all error types
- [ ] `CorrelationIdFilter` — `X-Correlation-ID` in MDC and response header
- [ ] `IdempotencyFilter` — cached responses for duplicate `Idempotency-Key`
- [ ] Request validation (`@Valid`, `@NotNull`, `@Future`)
- [ ] cURL examples for manual testing

## Implementation Details

### Package: `api.request`

#### `CreateAppointmentRequest.java`

```java
public record CreateAppointmentRequest(
    @NotNull Long customerId,
    @NotNull Long vehicleId,
    @NotNull Long dealershipId,
    @NotNull Long serviceTypeId,
    @NotNull @Future LocalDateTime startTime
) {}
```

### Package: `api.controller`

#### `AppointmentController.java`

```java
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

    // Constructor injection

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
```

#### `AvailabilityController.java`

```java
@RestController
@RequestMapping("/api/v1/dealerships")
public class AvailabilityController {

    private final GetAvailableSlotsQueryHandler availabilityHandler;

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
```

### Package: `api.exception`

#### `GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String PROBLEM_BASE_URI =
        "https://api.scheduler.keyloop.com/problems/";

    // Convert domain Error (from Result pattern) to ProblemDetail
    public static ProblemDetail fromDomainError(Error error, HttpStatus status,
                                                 String instance) {
        var problem = ProblemDetail.forStatusAndDetail(status, error.message());
        problem.setType(URI.create(PROBLEM_BASE_URI + toKebabCase(error.code())));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(instance));
        problem.setProperty("errorCode", error.code());
        problem.setProperty("correlationId", MDC.get("correlationId"));
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    // Map domain error code to HTTP status
    public static HttpStatus mapErrorToStatus(Error error) {
        var code = error.code();
        if (code.endsWith(".NotFound")) return HttpStatus.NOT_FOUND;
        if (code.contains("NotAvailable") || code.contains("Conflict"))
            return HttpStatus.CONFLICT;
        return HttpStatus.BAD_REQUEST;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException ex,
                                         HttpServletRequest request) { /* 404 */ }

    @ExceptionHandler(SlotNotAvailableException.class)
    public ProblemDetail handleConflict(SlotNotAvailableException ex,
                                         HttpServletRequest request) { /* 409 */ }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidationException(ValidationException ex,
                                                     HttpServletRequest request) {
        // 400 with per-field violations array
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // 400 with per-field violations from @Valid
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex,
                                           HttpServletRequest request) { /* 400 */ }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex,
                                        HttpServletRequest request) {
        // 500 — log.error(), return generic message (details not exposed)
    }
}
```

**Error mapping summary:**

| Exception / Error | HTTP Status | ProblemDetail type |
|-------------------|-------------|-------------------|
| `ValidationException` | 400 | `validation-error` (with `violations` array) |
| `MethodArgumentNotValidException` | 400 | `validation-error` |
| `IllegalArgumentException` | 400 | `invalid-request` |
| `EntityNotFoundException` | 404 | `entity-not-found` |
| `SlotNotAvailableException` | 409 | `slot-not-available` |
| Domain `Error` with `.NotFound` | 404 | error code (kebab-case) |
| Domain `Error` with `NotAvailable` | 409 | error code (kebab-case) |
| Unexpected `Exception` | 500 | `internal-error` |

### Package: `infrastructure.filter`

#### `CorrelationIdFilter.java`

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        var correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put("correlationId", correlationId);
        response.setHeader(HEADER, correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
```

#### `IdempotencyFilter.java`

```java
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String HEADER = "Idempotency-Key";
    private final SpringDataIdempotencyKeyRepository idempotencyRepo;

    // Only applies to POST requests
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        var key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            // Idempotency-Key required for POST
            response.setStatus(400);
            response.getWriter().write("Idempotency-Key header is required");
            return;
        }

        // Check if key was already processed
        var existing = idempotencyRepo.findById(key);
        if (existing.isPresent()) {
            // Return cached response
            var cached = existing.get();
            response.setStatus(cached.getResponseStatus());
            response.setContentType("application/json");
            response.getWriter().write(cached.getResponseBody());
            return;
        }

        // Wrap response to capture output
        var wrappedResponse = new ContentCachingResponseWrapper(response);
        chain.doFilter(request, wrappedResponse);

        // Store response for this key (24h TTL)
        var entity = new IdempotencyKeyJpaEntity();
        entity.setIdempotencyKey(key);
        entity.setResponseStatus(wrappedResponse.getStatus());
        entity.setResponseBody(new String(
            wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8));
        entity.setCreatedAt(LocalDateTime.now());
        idempotencyRepo.save(entity);

        wrappedResponse.copyBodyToResponse();
    }
}
```

### cURL Test Examples

```bash
# 1. Check availability (paginated)
curl -s "http://localhost:8080/api/v1/dealerships/1/availability\
?serviceTypeId=1&date=2025-03-20&pageIndex=0&pageSize=10" | jq

# 2. Create appointment (with Idempotency-Key)
curl -s -X POST http://localhost:8080/api/v1/appointments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
    "customerId": 1,
    "vehicleId": 1,
    "dealershipId": 1,
    "serviceTypeId": 1,
    "startTime": "2025-03-20T09:00:00"
  }' | jq

# 3. Get appointment
curl -s http://localhost:8080/api/v1/appointments/{id} | jq

# 4. Cancel appointment
curl -s -X DELETE http://localhost:8080/api/v1/appointments/{id}

# 5. Check in
curl -s -X PATCH http://localhost:8080/api/v1/appointments/{id}/check-in

# 6. Complete
curl -s -X PATCH http://localhost:8080/api/v1/appointments/{id}/complete

# 7. Mark no-show
curl -s -X PATCH http://localhost:8080/api/v1/appointments/{id}/no-show

# 8. Search appointments (paginated, filtered, sorted)
curl -s "http://localhost:8080/api/v1/appointments\
?customerId=1&status=CONFIRMED&sortBy=startTime&sortDirection=asc\
&pageIndex=0&pageSize=10" | jq

# 9. Duplicate idempotency key → returns cached response
curl -s -X POST http://localhost:8080/api/v1/appointments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: same-key-as-before" \
  -d '{ ... }' | jq
# Returns the same 201 response without creating a duplicate
```

## Commit message

```
feat(api): add REST controllers, exception handler, and filters
```
