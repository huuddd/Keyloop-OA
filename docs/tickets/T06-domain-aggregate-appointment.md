# T06 — Domain: Appointment Aggregate Root

**Type:** Feature | **Priority:** P1
**Depends on:** T04, T05
**Blocks:** T07, T09

---

## Description

Implement the `Appointment` aggregate root — the central domain object. Uses the `Result<T>` pattern (no exceptions), injected `Clock` for deterministic time, state machine transitions (`canTransitionTo`), and raises domain events for every lifecycle change.

## Acceptance Criteria

- [x] `Appointment` extends `AggregateRoot<AppointmentId>`
- [x] Factory method `Appointment.create(...)` returns `Result<Appointment>` (not constructor)
- [x] All state transitions use `Result<Void>`: `cancel()`, `checkIn()`, `complete()`, `markNoShow()`
- [x] All methods accept injected `Clock` (no `LocalDateTime.now()` calls)
- [x] Domain events raised: `AppointmentCreatedEvent`, `AppointmentCancelledEvent`, `AppointmentCheckedInEvent`, `AppointmentCompletedEvent`, `AppointmentNoShowEvent`
- [x] `reconstruct()` static method for hydrating from persistence
- [x] Unit tests for all valid transitions, invalid transitions, and invariant violations

## Implementation Details

### Package: `com.keyloop.scheduler.domain.appointment`

#### `Appointment.java`

```java
package com.keyloop.scheduler.domain.appointment;

import com.keyloop.scheduler.domain.shared.Result;
import com.keyloop.scheduler.domain.appointment.event.*;
import com.keyloop.scheduler.domain.shared.AggregateRoot;
import com.keyloop.scheduler.domain.customer.CustomerId;
import com.keyloop.scheduler.domain.dealership.ServiceBayId;
import com.keyloop.scheduler.domain.servicetype.ServiceType;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeId;
import com.keyloop.scheduler.domain.dealership.TechnicianId;
import com.keyloop.scheduler.domain.customer.VehicleId;
import com.keyloop.scheduler.domain.dealership.Technician;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * AGGREGATE ROOT.
 *
 * Invariants:
 *   1. endTime > startTime
 *   2. startTime must be in the future (at creation time)
 *   3. Technician must be qualified for the ServiceType
 *   4. Status transitions follow state machine (AppointmentStatus.canTransitionTo)
 *
 * Note: Double-booking prevention is NOT within this Aggregate —
 * it is a cross-aggregate constraint, handled by Application Service + DB lock.
 */
public class Appointment extends AggregateRoot<AppointmentId> {

    private AppointmentId id;
    private CustomerId customerId;
    private VehicleId vehicleId;
    private ServiceBayId serviceBayId;
    private TechnicianId technicianId;
    private ServiceTypeId serviceTypeId;
    private TimeSlot timeSlot;
    private AppointmentStatus status;

    private Appointment() {}

    /**
     * Factory method — creates a new Appointment.
     * Validates all domain invariants at creation time.
     *
     * @param clock injected clock for deterministic time
     * @return Result containing the new Appointment, or an Error
     */
    public static Result<Appointment> create(
            CustomerId customerId,
            VehicleId vehicleId,
            ServiceBayId serviceBayId,
            TechnicianId technicianId,
            ServiceType serviceType,
            Technician technician,
            LocalDateTime startTime,
            Clock clock) {

        // Invariant: Technician must be qualified
        if (!technician.isQualifiedFor(serviceType.getRequiredSpecialization())) {
            return Result.failure(AppointmentErrors.TECHNICIAN_NOT_QUALIFIED);
        }

        // Invariant: startTime must be in the future
        if (!startTime.isAfter(LocalDateTime.now(clock))) {
            return Result.failure(AppointmentErrors.START_TIME_IN_PAST);
        }

        var now = LocalDateTime.now(clock);
        var appointment = new Appointment();
        appointment.id = AppointmentId.generate();
        appointment.customerId = customerId;
        appointment.vehicleId = vehicleId;
        appointment.serviceBayId = serviceBayId;
        appointment.technicianId = technicianId;
        appointment.serviceTypeId = serviceType.getId();
        appointment.timeSlot = TimeSlot.of(startTime, serviceType.getDurationMinutes());
        appointment.status = AppointmentStatus.CONFIRMED;
        appointment.setCreatedAt(now);
        appointment.setUpdatedAt(now);

        // Raise domain event
        appointment.registerEvent(AppointmentCreatedEvent.of(
            appointment.id.value(), customerId.value(), serviceBayId.value(),
            technicianId.value(), appointment.getStartTime(), appointment.getEndTime()));

        return Result.success(appointment);
    }

    /** Cancel: CONFIRMED or IN_PROGRESS → CANCELLED */
    public Result<Void> cancel(Clock clock) {
        var guard = guardTransition(AppointmentStatus.CANCELLED);
        if (guard.isFailure()) return guard;
        this.status = AppointmentStatus.CANCELLED;
        setUpdatedAt(LocalDateTime.now(clock));
        registerEvent(AppointmentCancelledEvent.of(
            this.id.value(), this.customerId.value(), getUpdatedAt()));
        return Result.success();
    }

    /** Check in: CONFIRMED → IN_PROGRESS */
    public Result<Void> checkIn(Clock clock) {
        var guard = guardTransition(AppointmentStatus.IN_PROGRESS);
        if (guard.isFailure()) return guard;
        this.status = AppointmentStatus.IN_PROGRESS;
        setUpdatedAt(LocalDateTime.now(clock));
        registerEvent(AppointmentCheckedInEvent.of(this.id.value(), getUpdatedAt()));
        return Result.success();
    }

    /** Complete: IN_PROGRESS → COMPLETED */
    public Result<Void> complete(Clock clock) {
        var guard = guardTransition(AppointmentStatus.COMPLETED);
        if (guard.isFailure()) return guard;
        this.status = AppointmentStatus.COMPLETED;
        setUpdatedAt(LocalDateTime.now(clock));
        registerEvent(AppointmentCompletedEvent.of(this.id.value(), getUpdatedAt()));
        return Result.success();
    }

    /** Mark no-show: CONFIRMED → NO_SHOW */
    public Result<Void> markNoShow(Clock clock) {
        var guard = guardTransition(AppointmentStatus.NO_SHOW);
        if (guard.isFailure()) return guard;
        this.status = AppointmentStatus.NO_SHOW;
        setUpdatedAt(LocalDateTime.now(clock));
        registerEvent(AppointmentNoShowEvent.of(
            this.id.value(), this.customerId.value(), getUpdatedAt()));
        return Result.success();
    }

    private Result<Void> guardTransition(AppointmentStatus target) {
        if (!this.status.canTransitionTo(target)) {
            return Result.failure(
                AppointmentErrors.invalidTransition(this.status.name(), target.name()));
        }
        return Result.success();
    }

    // Getters (no setters — state change only via domain methods)
    @Override public AppointmentId getId() { return id; }
    public CustomerId getCustomerId() { return customerId; }
    public VehicleId getVehicleId() { return vehicleId; }
    public ServiceBayId getServiceBayId() { return serviceBayId; }
    public TechnicianId getTechnicianId() { return technicianId; }
    public ServiceTypeId getServiceTypeId() { return serviceTypeId; }
    public LocalDateTime getStartTime() { return timeSlot.startTime(); }
    public LocalDateTime getEndTime() { return timeSlot.endTime(); }
    public TimeSlot getTimeSlot() { return timeSlot; }
    public AppointmentStatus getStatus() { return status; }

    /** Reconstruct from persistence (no validation, no events). */
    public static Appointment reconstruct(
            AppointmentId id, CustomerId customerId, VehicleId vehicleId,
            ServiceBayId serviceBayId, TechnicianId technicianId, ServiceTypeId serviceTypeId,
            LocalDateTime startTime, LocalDateTime endTime,
            AppointmentStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        var apt = new Appointment();
        apt.id = id;
        apt.customerId = customerId;
        apt.vehicleId = vehicleId;
        apt.serviceBayId = serviceBayId;
        apt.technicianId = technicianId;
        apt.serviceTypeId = serviceTypeId;
        apt.timeSlot = new TimeSlot(startTime, endTime);
        apt.status = status;
        apt.setCreatedAt(createdAt);
        apt.setUpdatedAt(updatedAt);
        return apt;
    }
}
```

### Package: `com.keyloop.scheduler.domain.appointment.event`

#### `AppointmentCreatedEvent.java`

```java
package com.keyloop.scheduler.domain.appointment.event;

import com.keyloop.scheduler.domain.shared.DomainEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentCreatedEvent(
    UUID eventId, Instant occurredOn,
    UUID appointmentId, Long customerId, Long serviceBayId,
    Long technicianId, LocalDateTime startTime, LocalDateTime endTime
) implements DomainEvent {

    public static AppointmentCreatedEvent of(
            UUID appointmentId, Long customerId, Long serviceBayId,
            Long technicianId, LocalDateTime startTime, LocalDateTime endTime) {
        return new AppointmentCreatedEvent(
            UUID.randomUUID(), Instant.now(),
            appointmentId, customerId, serviceBayId,
            technicianId, startTime, endTime);
    }
}
```

#### `AppointmentCancelledEvent.java`

```java
package com.keyloop.scheduler.domain.appointment.event;

import com.keyloop.scheduler.domain.shared.DomainEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentCancelledEvent(
    UUID eventId, Instant occurredOn,
    UUID appointmentId, Long customerId, LocalDateTime cancelledAt
) implements DomainEvent {

    public static AppointmentCancelledEvent of(
            UUID appointmentId, Long customerId, LocalDateTime cancelledAt) {
        return new AppointmentCancelledEvent(
            UUID.randomUUID(), Instant.now(), appointmentId, customerId, cancelledAt);
    }
}
```

> Same pattern for: `AppointmentCheckedInEvent`, `AppointmentCompletedEvent`, `AppointmentNoShowEvent` — each captures the relevant data snapshot at transition time.

## Key Design Decisions

1. **Result<T> instead of exceptions** — domain failures (invalid transition, unqualified technician) are expected outcomes, not exceptional. Result forces callers to handle both paths.

2. **Clock injection** — every method that reads "now" takes a `Clock` parameter. Production injects `Clock.systemDefaultZone()`, tests inject `Clock.fixed(...)` for deterministic assertions.

3. **Domain events** — raised inside the aggregate via `registerEvent()`, published by infrastructure after successful persistence. Decouples side effects (logging, notifications) from the core state change.

4. **Aggregate boundary** — double-booking prevention is NOT an aggregate invariant. It's a cross-aggregate constraint (same bay can't have two overlapping appointments). Enforced at the application layer via pessimistic locking.

## Commit message

```
feat(domain): add Appointment aggregate root with Result pattern and events
```
