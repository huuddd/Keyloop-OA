# T03 — Flyway Migrations (Database Schema)

**Type:** Feature | **Priority:** P0
**Depends on:** T02
**Blocks:** T08

---

## Description

Create all Flyway migration scripts (V1–V5) that define the complete database schema, seed data, expanded appointment statuses, idempotency key storage, and audit/soft-delete columns.

## Acceptance Criteria

- [x] V1 — core schema: 8 tables with constraints, indexes, and partial indexes
- [x] V2 — seed data: 1 dealership, 3 bays, 4 service types, 5 technicians, 2 customers, 2 vehicles
- [x] V3 — expand appointment status CHECK constraint from 2 to 5 statuses; rebuild partial indexes
- [x] V4 — idempotency key table with expiry index
- [x] V5 — audit (createdBy, updatedBy) + soft delete (deleted, deletedAt, deletedBy) columns
- [x] `mvn spring-boot:run` — Flyway applies all 5 migrations, Hibernate validates schema

## Implementation Details

### `src/main/resources/db/migration/V1__create_schema.sql`

```sql
-- ============================
-- 1. Dealership
-- ============================
CREATE TABLE dealerships (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    address         VARCHAR(500),
    operating_start TIME NOT NULL DEFAULT '08:00',
    operating_end   TIME NOT NULL DEFAULT '17:00',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================
-- 2. Service Bay
-- ============================
CREATE TABLE service_bays (
    id              BIGSERIAL PRIMARY KEY,
    dealership_id   BIGINT NOT NULL REFERENCES dealerships(id),
    name            VARCHAR(100) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_service_bays_dealership ON service_bays(dealership_id);

-- ============================
-- 3. Service Type
-- ============================
CREATE TABLE service_types (
    id                      BIGSERIAL PRIMARY KEY,
    name                    VARCHAR(255) NOT NULL,
    duration_minutes        INT NOT NULL,
    required_specialization VARCHAR(100) NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================
-- 4. Technician
-- ============================
CREATE TABLE technicians (
    id              BIGSERIAL PRIMARY KEY,
    dealership_id   BIGINT NOT NULL REFERENCES dealerships(id),
    name            VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_technicians_dealership ON technicians(dealership_id);

-- ============================
-- 5. Technician ↔ Specialization (M:N via join table)
-- ============================
CREATE TABLE technician_specializations (
    technician_id           BIGINT NOT NULL REFERENCES technicians(id),
    specialization          VARCHAR(100) NOT NULL,
    PRIMARY KEY (technician_id, specialization)
);

-- ============================
-- 6. Customer
-- ============================
CREATE TABLE customers (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(50),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================
-- 7. Vehicle
-- ============================
CREATE TABLE vehicles (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT NOT NULL REFERENCES customers(id),
    make            VARCHAR(100) NOT NULL,
    model           VARCHAR(100) NOT NULL,
    year            INT NOT NULL,
    vin             VARCHAR(17) NOT NULL UNIQUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vehicles_customer ON vehicles(customer_id);

-- ============================
-- 8. Appointment (core table)
-- ============================
CREATE TABLE appointments (
    id              UUID PRIMARY KEY,
    customer_id     BIGINT NOT NULL REFERENCES customers(id),
    vehicle_id      BIGINT NOT NULL REFERENCES vehicles(id),
    service_bay_id  BIGINT NOT NULL REFERENCES service_bays(id),
    technician_id   BIGINT NOT NULL REFERENCES technicians(id),
    service_type_id BIGINT NOT NULL REFERENCES service_types(id),
    start_time      TIMESTAMP NOT NULL,
    end_time        TIMESTAMP NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_time_range CHECK (end_time > start_time),
    CONSTRAINT chk_status CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);

-- Partial indexes for availability check + contention query (Deep Dive #1)
CREATE INDEX idx_appointments_bay_time
    ON appointments(service_bay_id, start_time, end_time)
    WHERE status != 'CANCELLED';

CREATE INDEX idx_appointments_tech_time
    ON appointments(technician_id, start_time, end_time)
    WHERE status != 'CANCELLED';

CREATE INDEX idx_appointments_customer
    ON appointments(customer_id);
```

**Design notes:**
- Appointment PK is `UUID` — generated in domain layer, avoids DB sequence bottleneck
- Partial indexes exclude cancelled appointments — they don't participate in overlap checks
- `chk_time_range` enforces `endTime > startTime` at the database level
- Foreign keys enforce referential integrity for all relationships

### `src/main/resources/db/migration/V2__seed_data.sql`

```sql
-- 1 Dealership
INSERT INTO dealerships (id, name, address, operating_start, operating_end)
VALUES (1, 'Keyloop Motors Downtown', '123 Main Street', '08:00', '17:00');

-- 3 Service Bays
INSERT INTO service_bays (id, dealership_id, name) VALUES
(1, 1, 'Bay A'),
(2, 1, 'Bay B'),
(3, 1, 'Bay C');

-- 4 Service Types
INSERT INTO service_types (id, name, duration_minutes, required_specialization) VALUES
(1, 'Oil Change',        60,  'GENERAL'),
(2, 'Full Inspection',   120, 'GENERAL'),
(3, 'Brake Replacement', 90,  'BRAKES'),
(4, 'Engine Diagnostic', 60,  'ENGINE');

-- 5 Technicians
INSERT INTO technicians (id, dealership_id, name) VALUES
(1, 1, 'Alex Johnson'),
(2, 1, 'Maria Garcia'),
(3, 1, 'James Wilson'),
(4, 1, 'Sarah Chen'),
(5, 1, 'David Kim');

-- Technician specializations
INSERT INTO technician_specializations (technician_id, specialization) VALUES
(1, 'GENERAL'), (1, 'BRAKES'),
(2, 'GENERAL'), (2, 'ENGINE'),
(3, 'GENERAL'),
(4, 'GENERAL'), (4, 'BRAKES'), (4, 'ENGINE'),
(5, 'GENERAL');

-- 2 Customers
INSERT INTO customers (id, name, email, phone) VALUES
(1, 'John Doe',  'john@example.com',  '+1-555-0101'),
(2, 'Jane Smith', 'jane@example.com', '+1-555-0102');

-- 2 Vehicles
INSERT INTO vehicles (id, customer_id, make, model, year, vin) VALUES
(1, 1, 'Toyota',  'Camry',   2022, '1HGBH41JXMN109186'),
(2, 2, 'Honda',   'Civic',   2023, '2HGFC2F59MH123456');
```

**Seed data rationale:**
- 3 bays × 1 dealership — enough for concurrency testing (6 threads, 3 bays → at most 3 succeed)
- 5 technicians with varying specializations — tests specialization matching
- 4 service types covering 3 specializations (GENERAL, BRAKES, ENGINE) — tests qualification filtering

### `src/main/resources/db/migration/V3__add_appointment_statuses.sql`

```sql
-- Expand appointment status to support full lifecycle:
-- CONFIRMED → IN_PROGRESS → COMPLETED
--                         → CANCELLED (from CONFIRMED or IN_PROGRESS)
-- CONFIRMED → NO_SHOW

-- 1. Drop old constraint
ALTER TABLE appointments
    DROP CONSTRAINT chk_status;

-- 2. Add new constraint with all valid statuses
ALTER TABLE appointments
    ADD CONSTRAINT chk_status
    CHECK (status IN ('CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW'));

-- 3. Rebuild partial indexes to exclude all terminal statuses
DROP INDEX idx_appointments_bay_time;
DROP INDEX idx_appointments_tech_time;

CREATE INDEX idx_appointments_bay_time
    ON appointments(service_bay_id, start_time, end_time)
    WHERE status NOT IN ('CANCELLED', 'COMPLETED', 'NO_SHOW');

CREATE INDEX idx_appointments_tech_time
    ON appointments(technician_id, start_time, end_time)
    WHERE status NOT IN ('CANCELLED', 'COMPLETED', 'NO_SHOW');
```

### `src/main/resources/db/migration/V4__create_idempotency_keys.sql`

```sql
-- Idempotency key storage for preventing duplicate request processing.
-- Stores cached responses for POST/PUT requests with Idempotency-Key header.

CREATE TABLE idempotency_keys (
    idempotency_key       VARCHAR(64)   PRIMARY KEY,
    response_status       INTEGER       NOT NULL,
    response_body         TEXT,
    response_content_type VARCHAR(128),
    created_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    expires_at            TIMESTAMP     NOT NULL
);

CREATE INDEX idx_idempotency_keys_expires ON idempotency_keys (expires_at);
```

### `src/main/resources/db/migration/V5__add_audit_soft_delete_columns.sql`

```sql
-- Add audit (createdBy, updatedBy) and soft delete columns to all tables.

-- Appointments: already has created_at/updated_at, add the rest
ALTER TABLE appointments
    ADD COLUMN created_by VARCHAR(100),
    ADD COLUMN updated_by VARCHAR(100),
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deleted_by VARCHAR(100);

-- Customers: already has created_at from V1, add the rest
ALTER TABLE customers
    ADD COLUMN created_by VARCHAR(100),
    ADD COLUMN updated_at TIMESTAMP,
    ADD COLUMN updated_by VARCHAR(100),
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deleted_by VARCHAR(100);

-- Vehicles: already has created_at from V1, add the rest
ALTER TABLE vehicles
    ADD COLUMN created_by VARCHAR(100),
    ADD COLUMN updated_at TIMESTAMP,
    ADD COLUMN updated_by VARCHAR(100),
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deleted_by VARCHAR(100);

-- Partial indexes for efficient soft-delete filtering
CREATE INDEX idx_appointments_not_deleted ON appointments (id) WHERE deleted = false;
CREATE INDEX idx_customers_not_deleted ON customers (id) WHERE deleted = false;
CREATE INDEX idx_vehicles_not_deleted ON vehicles (id) WHERE deleted = false;
```

## Migration Evolution Summary

| Migration | Purpose | Tables affected |
|-----------|---------|----------------|
| V1 | Core schema — 8 tables with FK, CHECK, partial indexes | All |
| V2 | Seed data for development and testing | All |
| V3 | Expand status from 2 to 5 values; rebuild partial indexes | appointments |
| V4 | Idempotency key storage with TTL | idempotency_keys (new) |
| V5 | Audit trail + soft delete columns | appointments, customers, vehicles |

## Commit message

```
feat(db): add Flyway migrations V1-V5 with schema and seed data
```
