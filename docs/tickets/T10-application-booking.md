# T10 — Application: CQRS Command & Query Handlers

**Type:** Feature | **Priority:** P1
**Depends on:** T08, T09
**Blocks:** T11

---

## Description

Implement all CQRS command and query handlers for the appointment domain. The application layer uses the **Command/Query separation** pattern with `Result<T>` for error handling (no exceptions in the domain flow).

**5 Command Handlers** (mutate state):
- `CreateAppointmentCommandHandler` — resource-constrained booking with pessimistic locking
- `CancelAppointmentCommandHandler` — cancel with state machine validation
- `CheckInAppointmentCommandHandler` — CONFIRMED → IN_PROGRESS
- `CompleteAppointmentCommandHandler` — IN_PROGRESS → COMPLETED
- `MarkNoShowCommandHandler` — CONFIRMED → NO_SHOW

**4 Query Handlers** (read-only):
- `GetAppointmentQueryHandler` — get by ID
- `SearchAppointmentsQueryHandler` — paginated search with filters + sort
- `ListAppointmentsQueryHandler` — list by customer
- `GetAvailableSlotsQueryHandler` — (already in T09)

**Cross-cutting:**
- `ValidatingService` + `CommandValidator<T>` — validation pipeline before command execution
- `EventPublisher` port — publishes domain events after successful persistence
- `Clock` injection — deterministic time for testability

## Acceptance Criteria

- [ ] All 5 command handlers implement `CommandHandler<C, R>` returning `Result<R>`
- [ ] All query handlers implement `QueryHandler<Q, R>`
- [ ] `CreateAppointmentCommandHandler`: validate → lock → create → persist → publish events
- [ ] `CancelAppointmentCommandHandler`: find → cancel(clock) → persist → publish events
- [ ] `CheckInAppointmentCommandHandler`: find → checkIn(clock) → persist → publish events
- [ ] `CompleteAppointmentCommandHandler`: find → complete(clock) → persist → publish events
- [ ] `MarkNoShowCommandHandler`: find → markNoShow(clock) → persist → publish events
- [ ] `SearchAppointmentsQueryHandler`: delegates to `AppointmentRepository.searchAppointments()` with `PagedResult<AppointmentDto>`
- [ ] `ValidatingService` collects and runs all `CommandValidator<T>` implementations
- [ ] `CreateAppointmentCommandValidator` checks null fields + entity existence
- [ ] Unit tests for all handlers

## Implementation Details

### Package: `application.cqrs`

```java
public interface Command<R> {}
public interface CommandHandler<C extends Command<R>, R> {
    Result<R> handle(C command);
}

public interface Query<R> {}
public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}
```

### Package: `application.appointment.command`

#### `CreateAppointmentCommand.java`

```java
public record CreateAppointmentCommand(
    Long customerId,
    Long vehicleId,
    Long dealershipId,
    Long serviceTypeId,
    LocalDateTime startTime
) implements Command<AppointmentDto> {}
```

#### `CreateAppointmentCommandHandler.java`

```java
@Service
@Transactional
public class CreateAppointmentCommandHandler
        implements CommandHandler<CreateAppointmentCommand, AppointmentDto> {

    private final AppointmentRepository appointmentRepo;
    private final ServiceBayRepository serviceBayRepo;
    private final TechnicianRepository technicianRepo;
    private final ServiceTypeRepository serviceTypeRepo;
    private final EventPublisher eventPublisher;
    private final ValidatingService validatingService;
    private final Clock clock;

    // Constructor injection

    @Override
    public Result<AppointmentDto> handle(CreateAppointmentCommand command) {

        // 1. Validate command (null checks + existence checks)
        var validation = validatingService.validate(command);
        if (!validation.isValid()) {
            throw new ValidationException(validation);
        }

        // Anti-corruption layer: wrap raw IDs into typed IDs
        var customerId = CustomerId.of(command.customerId());
        var vehicleId = VehicleId.of(command.vehicleId());
        var dealershipId = DealershipId.of(command.dealershipId());
        var serviceTypeId = ServiceTypeId.of(command.serviceTypeId());

        var serviceType = serviceTypeRepo.findById(serviceTypeId).orElseThrow();
        var endTime = command.startTime().plusMinutes(serviceType.getDurationMinutes());

        // 2. Find available bay with pessimistic lock
        var bays = serviceBayRepo.findActiveByDealershipId(dealershipId);
        ServiceBayId selectedBayId = null;

        for (var bay : bays) {
            var overlapping = appointmentRepo.findOverlappingForBayWithLock(
                bay.getId(), command.startTime(), endTime);
            if (overlapping.isEmpty()) {
                selectedBayId = bay.getId();
                break;
            }
        }

        if (selectedBayId == null) {
            return Result.failure(AppointmentErrors.NO_BAY_AVAILABLE);
        }

        // 3. Find available qualified technician with pessimistic lock
        var technicians = technicianRepo.findByDealershipIdAndSpecialization(
            dealershipId, serviceType.getRequiredSpecialization());
        Technician selectedTech = null;

        for (var tech : technicians) {
            var overlapping = appointmentRepo.findOverlappingForTechnicianWithLock(
                tech.getId(), command.startTime(), endTime);
            if (overlapping.isEmpty()) {
                selectedTech = tech;
                break;
            }
        }

        if (selectedTech == null) {
            return Result.failure(AppointmentErrors.NO_TECHNICIAN_AVAILABLE);
        }

        // 4. Create domain object (validates invariants via Result)
        var createResult = Appointment.create(
            customerId, vehicleId, selectedBayId,
            selectedTech.getId(), serviceType, selectedTech,
            command.startTime(), clock);

        if (createResult.isFailure()) {
            return Result.failure(createResult.getError());
        }

        var appointment = createResult.getValue();

        // 5. Persist
        var saved = appointmentRepo.save(appointment);

        // 6. Publish domain events after successful persistence
        eventPublisher.publishEventsFor(appointment);

        return Result.success(AppointmentDto.fromDomain(saved));
    }
}
```

#### `CancelAppointmentCommandHandler.java`

```java
@Service
@Transactional
public class CancelAppointmentCommandHandler
        implements CommandHandler<CancelAppointmentCommand, Void> {

    private final AppointmentRepository appointmentRepo;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    // Constructor injection

    @Override
    public Result<Void> handle(CancelAppointmentCommand command) {
        var appointmentId = AppointmentId.of(command.appointmentId());
        var appointment = appointmentRepo.findById(appointmentId).orElse(null);
        if (appointment == null) {
            return Result.failure(AppointmentErrors.notFound(appointmentId));
        }

        var cancelResult = appointment.cancel(clock);
        if (cancelResult.isFailure()) {
            return cancelResult;
        }

        appointmentRepo.save(appointment);
        eventPublisher.publishEventsFor(appointment);

        return Result.success();
    }
}
```

Similarly: `CheckInAppointmentCommandHandler`, `CompleteAppointmentCommandHandler`, `MarkNoShowCommandHandler` — all follow the same pattern: find → domain method(clock) → check Result → persist → publish events.

#### Command records

```java
public record CancelAppointmentCommand(UUID appointmentId) implements Command<Void> {}
public record CheckInAppointmentCommand(UUID appointmentId) implements Command<Void> {}
public record CompleteAppointmentCommand(UUID appointmentId) implements Command<Void> {}
public record MarkNoShowCommand(UUID appointmentId) implements Command<Void> {}
```

### Package: `application.appointment.query`

#### `GetAppointmentQueryHandler.java`

```java
@Service
@Transactional(readOnly = true)
public class GetAppointmentQueryHandler
        implements QueryHandler<GetAppointmentQuery, AppointmentDto> {

    private final AppointmentRepository appointmentRepo;

    @Override
    public AppointmentDto handle(GetAppointmentQuery query) {
        var id = AppointmentId.of(query.appointmentId());
        return appointmentRepo.findById(id)
            .map(AppointmentDto::fromDomain)
            .orElseThrow(() -> new EntityNotFoundException(
                "Appointment not found: " + query.appointmentId()));
    }
}
```

#### `SearchAppointmentsQueryHandler.java`

```java
@Service
@Transactional(readOnly = true)
public class SearchAppointmentsQueryHandler
        implements QueryHandler<SearchAppointmentsQuery, PagedResult<AppointmentDto>> {

    private final AppointmentRepository appointmentRepo;

    @Override
    public PagedResult<AppointmentDto> handle(SearchAppointmentsQuery query) {
        var result = appointmentRepo.searchAppointments(
            query.customerId(), query.status(),
            query.dateFrom(), query.dateTo(),
            query.sortBy(), query.sortDirection(),
            query.pageIndex(), query.pageSize());

        var dtos = result.items().stream()
            .map(AppointmentDto::fromDomain).toList();

        return new PagedResult<>(dtos, result.totalCount(),
            query.pageIndex(), query.pageSize());
    }
}
```

### Package: `application.validation`

#### `CommandValidator.java`

```java
public interface CommandValidator<T> {
    ValidationResult validate(T command);
    Class<T> getCommandType();
}
```

#### `ValidatingService.java`

```java
@Service
public class ValidatingService {

    private final Map<Class<?>, List<CommandValidator<?>>> validatorMap;

    public ValidatingService(List<CommandValidator<?>> validators) {
        this.validatorMap = validators.stream()
            .collect(Collectors.groupingBy(CommandValidator::getCommandType));
    }

    @SuppressWarnings("unchecked")
    public <T> ValidationResult validate(T command) {
        var validators = validatorMap.getOrDefault(command.getClass(), List.of());
        return validators.stream()
            .map(v -> ((CommandValidator<T>) v).validate(command))
            .reduce(ValidationResult::merge)
            .orElse(ValidationResult.valid());
    }
}
```

#### `CreateAppointmentCommandValidator.java`

Validates: all fields non-null, startTime in the future, all referenced entities (customer, vehicle, dealership, serviceType) exist in the database.

### Package: `application.port`

#### `EventPublisher.java`

```java
public interface EventPublisher {
    void publishEventsFor(AggregateRoot<?> aggregateRoot);
}
```

### Package: `application.dto`

#### `AppointmentDto.java`

```java
public record AppointmentDto(
    UUID id,
    Long customerId,
    Long vehicleId,
    Long dealershipId,
    Long serviceBayId,
    Long technicianId,
    Long serviceTypeId,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String status,
    String cancellationReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AppointmentDto fromDomain(Appointment appointment) {
        return new AppointmentDto(
            appointment.getId().value(),
            appointment.getCustomerId().value(),
            appointment.getVehicleId().value(),
            appointment.getDealershipId().value(),
            appointment.getServiceBayId().value(),
            appointment.getTechnicianId().value(),
            appointment.getServiceTypeId().value(),
            appointment.getStartTime(),
            appointment.getEndTime(),
            appointment.getStatus().name(),
            appointment.getCancellationReason(),
            appointment.getCreatedAt(),
            appointment.getUpdatedAt());
    }
}
```

### Package: `application.event`

Domain event handlers (listeners via Spring `@EventListener`):

```java
@Component
public class AppointmentCreatedEventHandler {
    private static final Logger log = LoggerFactory.getLogger(...);

    @EventListener
    public void handle(AppointmentCreatedEvent event) {
        log.info("Appointment created: id={}, bay={}, tech={}, time={}-{}",
            event.appointmentId(), event.serviceBayId(),
            event.technicianId(), event.startTime(), event.endTime());
    }
}
```

Similarly: `AppointmentCancelledEventHandler`, `AppointmentCheckedInEventHandler`, `AppointmentCompletedEventHandler`, `AppointmentNoShowEventHandler`.

## Commit message

```
feat(app): add CQRS command/query handlers with validation pipeline
```
