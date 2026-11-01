# T05 — Domain: Entities (Dealership, ServiceBay, Technician, ServiceType, Customer, Vehicle)

**Type:** Feature | **Priority:** P1
**Depends on:** T04
**Blocks:** T06

---

## Description

Create the supporting domain entities organized by aggregate: `dealership` (Dealership, ServiceBay, Technician), `servicetype` (ServiceType), and `customer` (Customer, Vehicle). All entities extend the `Entity<TId>` base class and use strongly-typed IDs. The domain layer is pure Java with zero framework dependencies.

## Acceptance Criteria

- [ ] `Dealership` with `generatePossibleStartTimes()` slot calculation
- [ ] `ServiceBay` with active/inactive flag
- [ ] `Technician` with specializations set and `isQualifiedFor()` business rule
- [ ] `ServiceType` with duration and required specialization
- [ ] `Customer` and `Vehicle` entities
- [ ] All entities use strongly-typed IDs
- [ ] All entities extend `Entity<TId>` (not AggregateRoot — only Appointment is an aggregate root)

## Implementation Details

### Package: `com.keyloop.scheduler.domain.dealership`

#### `Dealership.java`

```java
package com.keyloop.scheduler.domain.dealership;

import com.keyloop.scheduler.domain.shared.Entity;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Dealership extends Entity<DealershipId> {

    private DealershipId id;
    private String name;
    private String address;
    private LocalTime operatingStart;
    private LocalTime operatingEnd;

    public Dealership(DealershipId id, String name, String address,
                      LocalTime operatingStart, LocalTime operatingEnd) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.operatingStart = operatingStart;
        this.operatingEnd = operatingEnd;
    }

    protected Dealership() {}

    /**
     * Generate all possible appointment start times for a day.
     * Uses 30-minute slot granularity.
     * The last slot must end at or before operatingEnd.
     *
     * Example: operating 08:00-17:00, duration 90 min, granularity 30 min
     * → [08:00, 08:30, 09:00, ..., 15:30] (15:30 + 90 = 17:00 ≤ closing)
     */
    public List<LocalTime> generatePossibleStartTimes(int durationMinutes) {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime current = operatingStart;
        int granularity = 30;

        while (!current.plusMinutes(durationMinutes).isAfter(operatingEnd)) {
            slots.add(current);
            current = current.plusMinutes(granularity);
        }
        return slots;
    }

    @Override
    public DealershipId getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public LocalTime getOperatingStart() { return operatingStart; }
    public LocalTime getOperatingEnd() { return operatingEnd; }
}
```

#### `ServiceBay.java`

```java
package com.keyloop.scheduler.domain.dealership;

import com.keyloop.scheduler.domain.shared.Entity;

public class ServiceBay extends Entity<ServiceBayId> {

    private ServiceBayId id;
    private DealershipId dealershipId;
    private String name;
    private boolean active;

    public ServiceBay(ServiceBayId id, DealershipId dealershipId, String name, boolean active) {
        this.id = id;
        this.dealershipId = dealershipId;
        this.name = name;
        this.active = active;
    }

    protected ServiceBay() {}

    @Override
    public ServiceBayId getId() { return id; }
    public DealershipId getDealershipId() { return dealershipId; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
```

#### `Technician.java`

```java
package com.keyloop.scheduler.domain.dealership;

import com.keyloop.scheduler.domain.shared.Entity;
import com.keyloop.scheduler.domain.servicetype.Specialization;

import java.util.Collections;
import java.util.Set;

public class Technician extends Entity<TechnicianId> {

    private TechnicianId id;
    private DealershipId dealershipId;
    private String name;
    private Set<Specialization> specializations;

    public Technician(TechnicianId id, DealershipId dealershipId, String name,
                      Set<Specialization> specializations) {
        this.id = id;
        this.dealershipId = dealershipId;
        this.name = name;
        this.specializations = specializations;
    }

    protected Technician() {}

    /**
     * Business rule: can this technician handle the given service type?
     */
    public boolean isQualifiedFor(Specialization required) {
        return specializations.contains(required);
    }

    @Override
    public TechnicianId getId() { return id; }
    public DealershipId getDealershipId() { return dealershipId; }
    public String getName() { return name; }
    public Set<Specialization> getSpecializations() {
        return Collections.unmodifiableSet(specializations);
    }
}
```

### Package: `com.keyloop.scheduler.domain.servicetype`

#### `ServiceType.java`

```java
package com.keyloop.scheduler.domain.servicetype;

import com.keyloop.scheduler.domain.shared.Entity;

public class ServiceType extends Entity<ServiceTypeId> {

    private ServiceTypeId id;
    private String name;
    private int durationMinutes;
    private Specialization requiredSpecialization;

    public ServiceType(ServiceTypeId id, String name, int durationMinutes,
                       Specialization requiredSpecialization) {
        this.id = id;
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.requiredSpecialization = requiredSpecialization;
    }

    protected ServiceType() {}

    @Override
    public ServiceTypeId getId() { return id; }
    public String getName() { return name; }
    public int getDurationMinutes() { return durationMinutes; }
    public Specialization getRequiredSpecialization() { return requiredSpecialization; }
}
```

### Package: `com.keyloop.scheduler.domain.customer`

#### `Customer.java`

```java
package com.keyloop.scheduler.domain.customer;

import com.keyloop.scheduler.domain.shared.Entity;

public class Customer extends Entity<CustomerId> {

    private CustomerId id;
    private String name;
    private String email;
    private String phone;

    public Customer(CustomerId id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    protected Customer() {}

    @Override
    public CustomerId getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
}
```

#### `Vehicle.java`

```java
package com.keyloop.scheduler.domain.customer;

import com.keyloop.scheduler.domain.shared.Entity;

public class Vehicle extends Entity<VehicleId> {

    private VehicleId id;
    private CustomerId customerId;
    private String make;
    private String model;
    private int year;
    private String vin;

    public Vehicle(VehicleId id, CustomerId customerId, String make,
                   String model, int year, String vin) {
        this.id = id;
        this.customerId = customerId;
        this.make = make;
        this.model = model;
        this.year = year;
        this.vin = vin;
    }

    protected Vehicle() {}

    @Override
    public VehicleId getId() { return id; }
    public CustomerId getCustomerId() { return customerId; }
    public String getMake() { return make; }
    public String getModel() { return model; }
    public int getYear() { return year; }
    public String getVin() { return vin; }
}
```

## Design Notes

- **Package-by-aggregate:** entities live in their aggregate's package, not a flat `domain.model` folder
- **No setters:** state changes only through domain methods (encapsulation)
- **Protected no-arg constructors:** required by JPA/Hibernate for proxy instantiation, but not exposed publicly
- **Strongly-typed IDs everywhere:** prevents parameter ordering mistakes (`CustomerId` can never be passed where `VehicleId` is expected)
- **`Technician.isQualifiedFor()`:** the only business rule on supporting entities — checks if a technician has the required specialization for a service type

## Commit message

```
feat(domain): add supporting entities with strongly-typed IDs
```
