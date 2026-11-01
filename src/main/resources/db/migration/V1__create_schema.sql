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
-- 5. Technician <-> Specialization (M:N via join table)
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
