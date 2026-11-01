# T07 — Domain: Repository Interfaces (Ports)

**Type:** Feature | **Priority:** P1
**Depends on:** T06
**Blocks:** T08

---

## Description

Define repository interfaces (ports) in the domain layer for each aggregate. These are pure Java interfaces — no Spring, no JPA annotations. The infrastructure layer (T08) provides the implementations (adapters). All methods use strongly-typed IDs.

## Acceptance Criteria

- [ ] `AppointmentRepository` — CRUD + overlap queries (with and without pessimistic lock) + paged search
- [ ] `CustomerRepository`, `VehicleRepository` — findById
- [ ] `DealershipRepository`, `ServiceBayRepository`, `TechnicianRepository` — findById + aggregate-specific queries
- [ ] `ServiceTypeRepository` — findById
- [ ] All interfaces use strongly-typed IDs (not raw Long/UUID)
- [ ] Each interface lives in its aggregate's package

## Implementation Details

### Package: `com.keyloop.scheduler.domain.appointment`

#### `AppointmentRepository.java`

```java
package com.keyloop.scheduler.domain.appointment;

import com.keyloop.scheduler.domain.customer.CustomerId;
import com.keyloop.scheduler.domain.dealership.DealershipId;
import com.keyloop.scheduler.domain.dealership.ServiceBayId;
import com.keyloop.scheduler.domain.dealership.TechnicianId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository {

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(AppointmentId id);

    List<Appointment> findByCustomerId(CustomerId customerId);

    /** Overlap check (no lock) — used for availability queries. */
    List<Appointment> findOverlappingForBay(
        ServiceBayId serviceBayId, LocalDateTime startTime, LocalDateTime endTime);

    List<Appointment> findOverlappingForTechnician(
        TechnicianId technicianId, LocalDateTime startTime, LocalDateTime endTime);

    /** Pessimistic lock version — SELECT FOR UPDATE (Deep Dive #1). */
    List<Appointment> findOverlappingForBayWithLock(
        ServiceBayId serviceBayId, LocalDateTime startTime, LocalDateTime endTime);

    List<Appointment> findOverlappingForTechnicianWithLock(
        TechnicianId technicianId, LocalDateTime startTime, LocalDateTime endTime);

    /** All appointments for a dealership on a given date (availability calculation). */
    List<Appointment> findByDealershipAndDate(DealershipId dealershipId, LocalDate date);

    /** Paged search with optional filters and sorting. */
    SearchResult searchAppointments(
        Long customerId, String status,
        LocalDate dateFrom, LocalDate dateTo,
        String sortBy, String sortDirection,
        int pageIndex, int pageSize);

    record SearchResult(List<Appointment> items, long totalCount) {}
}
```

### Package: `com.keyloop.scheduler.domain.customer`

#### `CustomerRepository.java`

```java
package com.keyloop.scheduler.domain.customer;

import java.util.Optional;

public interface CustomerRepository {
    Optional<Customer> findById(CustomerId id);
}
```

#### `VehicleRepository.java`

```java
package com.keyloop.scheduler.domain.customer;

import java.util.Optional;

public interface VehicleRepository {
    Optional<Vehicle> findById(VehicleId id);
}
```

### Package: `com.keyloop.scheduler.domain.dealership`

#### `DealershipRepository.java`

```java
package com.keyloop.scheduler.domain.dealership;

import java.util.Optional;

public interface DealershipRepository {
    Optional<Dealership> findById(DealershipId id);
}
```

#### `ServiceBayRepository.java`

```java
package com.keyloop.scheduler.domain.dealership;

import java.util.List;

public interface ServiceBayRepository {
    List<ServiceBay> findActiveByDealershipId(DealershipId dealershipId);
}
```

#### `TechnicianRepository.java`

```java
package com.keyloop.scheduler.domain.dealership;

import com.keyloop.scheduler.domain.servicetype.Specialization;
import java.util.List;

public interface TechnicianRepository {
    List<Technician> findByDealershipIdAndSpecialization(
        DealershipId dealershipId, Specialization specialization);
}
```

### Package: `com.keyloop.scheduler.domain.servicetype`

#### `ServiceTypeRepository.java`

```java
package com.keyloop.scheduler.domain.servicetype;

import java.util.Optional;

public interface ServiceTypeRepository {
    Optional<ServiceType> findById(ServiceTypeId id);
}
```

## Design Notes

- **Ports & Adapters (Hexagonal Architecture):** repository interfaces are ports in the domain. The infrastructure layer implements them as adapters using Spring Data JPA.
- **No pagination types from Spring:** the domain defines its own `SearchResult` record, keeping it framework-free. The infrastructure adapter maps from `Page<T>` to `SearchResult`.
- **Pessimistic lock split:** separate methods with/without lock so callers are explicit about intent (availability check = no lock, booking = lock).

## Commit message

```
feat(domain): add repository interfaces (ports) for all aggregates
```
