# T04 — Domain: Shared Base Classes, Enums, Value Objects, Result Pattern

**Type:** Feature | **Priority:** P1
**Depends on:** T01
**Blocks:** T05, T06

---

## Description

Build the domain layer foundation: the `Entity` → `AuditableEntity` → `SoftDeletableEntity` → `AggregateRoot` hierarchy in `domain.shared`, the `Result<T>` / `Error` types for error handling without exceptions, the `DomainEvent` marker interface, strongly-typed ID records, the `AppointmentStatus` enum with state machine transitions, the `Specialization` enum, and the `TimeSlot` value object.

All code in the `domain` package is **pure Java** — zero framework dependencies.

## Acceptance Criteria

- [x] Entity hierarchy: `Entity<TId>` → `AuditableEntity<TId>` → `SoftDeletableEntity<TId>` → `AggregateRoot<TId>`
- [x] `Result<T>` sealed interface with `Success` / `Failure` records
- [x] `Error` record with code + message
- [x] `DomainEvent` marker interface with `occurredOn()`
- [x] `Auditable` + `SoftDeletable` interfaces
- [x] Strongly-typed IDs: `AppointmentId`, `CustomerId`, `VehicleId`, `DealershipId`, `ServiceBayId`, `TechnicianId`, `ServiceTypeId`
- [x] `AppointmentStatus` enum with `canTransitionTo()` state machine
- [x] `Specialization` enum (GENERAL, BRAKES, ENGINE)
- [x] `TimeSlot` value object with `overlapsWith()` logic
- [x] Unit tests for state transitions and TimeSlot overlap

## Implementation Details

### Package: `com.keyloop.scheduler.domain.shared`

#### `Entity.java`

```java
package com.keyloop.scheduler.domain.shared;

import java.util.Objects;

public abstract class Entity<TId> {

    public abstract TId getId();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity<?> entity = (Entity<?>) o;
        return getId() != null && getId().equals(entity.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? Objects.hash(getId()) : super.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + getId() + "]";
    }
}
```

> Identity-based equality: two entities are equal if and only if they have the same type and ID.

#### `Auditable.java`

```java
package com.keyloop.scheduler.domain.shared;

import java.time.LocalDateTime;

public interface Auditable {
    LocalDateTime getCreatedAt();
    String getCreatedBy();
    LocalDateTime getUpdatedAt();
    String getUpdatedBy();
}
```

#### `AuditableEntity.java`

```java
package com.keyloop.scheduler.domain.shared;

import java.time.LocalDateTime;

public abstract class AuditableEntity<TId> extends Entity<TId> implements Auditable {

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    @Override public LocalDateTime getCreatedAt() { return createdAt; }
    @Override public String getCreatedBy() { return createdBy; }
    @Override public LocalDateTime getUpdatedAt() { return updatedAt; }
    @Override public String getUpdatedBy() { return updatedBy; }

    protected void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    protected void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    protected void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    protected void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
```

> Audit fields are populated by infrastructure-layer JPA listeners, keeping this class free of framework dependencies.

#### `SoftDeletable.java`

```java
package com.keyloop.scheduler.domain.shared;

import java.time.LocalDateTime;

public interface SoftDeletable {
    boolean isDeleted();
    LocalDateTime getDeletedAt();
    String getDeletedBy();
    void softDelete(String deletedBy);
    void restore();
}
```

#### `SoftDeletableEntity.java`

```java
package com.keyloop.scheduler.domain.shared;

import java.time.Clock;
import java.time.LocalDateTime;

public abstract class SoftDeletableEntity<TId> extends AuditableEntity<TId>
        implements SoftDeletable {

    private boolean deleted = false;
    private LocalDateTime deletedAt;
    private String deletedBy;

    @Override public boolean isDeleted() { return deleted; }
    @Override public LocalDateTime getDeletedAt() { return deletedAt; }
    @Override public String getDeletedBy() { return deletedBy; }

    @Override
    public void softDelete(String deletedBy) {
        if (this.deleted) throw new IllegalStateException("Entity is already deleted");
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    public void softDelete(String deletedBy, Clock clock) {
        if (this.deleted) throw new IllegalStateException("Entity is already deleted");
        this.deleted = true;
        this.deletedAt = LocalDateTime.now(clock);
        this.deletedBy = deletedBy;
    }

    @Override
    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }
}
```

#### `DomainEvent.java`

```java
package com.keyloop.scheduler.domain.shared;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredOn();
}
```

#### `AggregateRoot.java`

```java
package com.keyloop.scheduler.domain.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AggregateRoot<TId> extends SoftDeletableEntity<TId> {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
```

> Aggregate roots are the only entry point for modifications within their aggregate boundary. They collect domain events that are published after the unit of work commits.

#### `Result.java`

```java
package com.keyloop.scheduler.domain.shared;

import java.util.Objects;
import java.util.function.Function;

public sealed interface Result<T> {

    boolean isSuccess();
    boolean isFailure();
    T getValue();
    Error getError();

    <R> R fold(Function<T, R> onSuccess, Function<Error, R> onFailure);

    static <T> Result<T> success(T value) { return new Success<>(value); }
    static <T> Result<T> failure(Error error) { return new Failure<>(error); }
    static Result<Void> success() { return new Success<>(null); }

    record Success<T>(T value) implements Result<T> {
        @Override public boolean isSuccess() { return true; }
        @Override public boolean isFailure() { return false; }
        @Override public T getValue() { return value; }
        @Override public Error getError() {
            throw new IllegalStateException("No error on success result");
        }
        @Override
        public <R> R fold(Function<T, R> onSuccess, Function<Error, R> onFailure) {
            return onSuccess.apply(value);
        }
    }

    record Failure<T>(Error error) implements Result<T> {
        public Failure { Objects.requireNonNull(error, "Error must not be null"); }
        @Override public boolean isSuccess() { return false; }
        @Override public boolean isFailure() { return true; }
        @Override public T getValue() {
            throw new IllegalStateException("No value on failure result: " + error);
        }
        @Override public Error getError() { return error; }
        @Override
        public <R> R fold(Function<T, R> onSuccess, Function<Error, R> onFailure) {
            return onFailure.apply(error);
        }
    }
}
```

> Replaces exception-based flow control for expected domain failures. Controllers use `result.fold(...)` to map success/failure to HTTP responses.

#### `Error.java`

```java
package com.keyloop.scheduler.domain.shared;

public record Error(String code, String message) {
    public static final Error NONE = new Error("", "");
}
```

> Error codes follow the format `"Entity.Reason"`, e.g., `"Appointment.TechnicianNotQualified"`.

### Package: `com.keyloop.scheduler.domain.appointment`

#### `AppointmentId.java`

```java
package com.keyloop.scheduler.domain.appointment;

import java.util.Objects;
import java.util.UUID;

public record AppointmentId(UUID value) {
    public AppointmentId {
        Objects.requireNonNull(value, "AppointmentId value must not be null");
    }
    public static AppointmentId generate() { return new AppointmentId(UUID.randomUUID()); }
    public static AppointmentId of(UUID value) { return new AppointmentId(value); }
    @Override public String toString() { return value.toString(); }
}
```

> All typed IDs follow the same pattern. For `Long`-based IDs (CustomerId, VehicleId, etc.), the record validates positivity:

#### `CustomerId.java` (representative pattern for Long-based IDs)

```java
package com.keyloop.scheduler.domain.customer;

import java.util.Objects;

public record CustomerId(Long value) {
    public CustomerId {
        Objects.requireNonNull(value, "CustomerId value must not be null");
        if (value <= 0) throw new IllegalArgumentException("CustomerId must be positive");
    }
    public static CustomerId of(Long value) { return new CustomerId(value); }
    @Override public String toString() { return value.toString(); }
}
```

> Same pattern for: `VehicleId`, `DealershipId`, `ServiceBayId`, `TechnicianId`, `ServiceTypeId` — all in their respective aggregate packages.

#### `AppointmentStatus.java`

```java
package com.keyloop.scheduler.domain.appointment;

public enum AppointmentStatus {
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    public boolean canTransitionTo(AppointmentStatus target) {
        return switch (this) {
            case CONFIRMED -> target == IN_PROGRESS
                           || target == CANCELLED
                           || target == NO_SHOW;
            case IN_PROGRESS -> target == COMPLETED
                             || target == CANCELLED;
            case COMPLETED, CANCELLED, NO_SHOW -> false;
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == NO_SHOW;
    }
}
```

**State diagram:**
```
[*] → CONFIRMED → IN_PROGRESS → COMPLETED → [*]
                → CANCELLED   → [*]
                → NO_SHOW     → [*]
      IN_PROGRESS → CANCELLED → [*]
```

#### `TimeSlot.java`

```java
package com.keyloop.scheduler.domain.appointment;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public record TimeSlot(LocalDateTime startTime, LocalDateTime endTime) {

    public TimeSlot {
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
    }

    public boolean overlapsWith(TimeSlot other) {
        return this.startTime.isBefore(other.endTime)
            && this.endTime.isAfter(other.startTime);
    }

    public static TimeSlot of(LocalDateTime startTime, int durationMinutes) {
        return new TimeSlot(startTime, startTime.plusMinutes(durationMinutes));
    }

    public int durationMinutes() {
        return (int) Duration.between(startTime, endTime).toMinutes();
    }
}
```

> Overlap formula: `this.start < other.end AND this.end > other.start`. Used in both availability queries and domain validation.

### Package: `com.keyloop.scheduler.domain.servicetype`

#### `Specialization.java`

```java
package com.keyloop.scheduler.domain.servicetype;

public enum Specialization {
    GENERAL,
    BRAKES,
    ENGINE
}
```

#### `AppointmentErrors.java`

```java
package com.keyloop.scheduler.domain.appointment;

import com.keyloop.scheduler.domain.shared.Error;

public final class AppointmentErrors {

    private AppointmentErrors() {}

    public static final Error TECHNICIAN_NOT_QUALIFIED = new Error(
        "Appointment.TechnicianNotQualified",
        "Technician is not qualified for the requested service type");

    public static final Error START_TIME_IN_PAST = new Error(
        "Appointment.StartTimeInPast",
        "Appointment start time must be in the future");

    public static final Error ALREADY_CANCELLED = new Error(
        "Appointment.AlreadyCancelled",
        "Appointment is already cancelled");

    public static Error invalidTransition(String from, String to) {
        return new Error(
            "Appointment.InvalidTransition",
            "Cannot transition from " + from + " to " + to);
    }

    public static Error notFound(AppointmentId id) {
        return new Error("Appointment.NotFound", "Appointment not found: " + id);
    }

    public static final Error NO_BAY_AVAILABLE = new Error(
        "Appointment.NoBayAvailable",
        "No service bay available at the requested time");

    public static final Error NO_TECHNICIAN_AVAILABLE = new Error(
        "Appointment.NoTechnicianAvailable",
        "No qualified technician available at the requested time");
}
```

## Domain Package Structure

```
domain/
├── appointment/
│   ├── Appointment.java           (T06)
│   ├── AppointmentId.java
│   ├── AppointmentStatus.java
│   ├── AppointmentErrors.java
│   ├── AppointmentRepository.java (T07)
│   ├── TimeSlot.java
│   └── event/                     (T06)
├── servicetype/
│   ├── ServiceType.java           (T05)
│   ├── ServiceTypeId.java
│   ├── ServiceTypeRepository.java (T07)
│   └── Specialization.java
├── customer/
│   ├── Customer.java              (T05)
│   ├── CustomerId.java
│   ├── CustomerRepository.java    (T07)
│   ├── Vehicle.java               (T05)
│   ├── VehicleId.java
│   └── VehicleRepository.java     (T07)
├── dealership/
│   ├── Dealership.java            (T05)
│   ├── DealershipId.java
│   ├── DealershipRepository.java  (T07)
│   ├── ServiceBay.java            (T05)
│   ├── ServiceBayId.java
│   ├── ServiceBayRepository.java  (T07)
│   ├── Technician.java            (T05)
│   ├── TechnicianId.java
│   └── TechnicianRepository.java  (T07)
└── shared/
    ├── Entity.java
    ├── AuditableEntity.java
    ├── SoftDeletableEntity.java
    ├── AggregateRoot.java
    ├── Auditable.java
    ├── SoftDeletable.java
    ├── DomainEvent.java
    ├── Result.java
    └── Error.java
```

## Commit message

```
feat(domain): add shared base classes, typed IDs, enums, and value objects
```
