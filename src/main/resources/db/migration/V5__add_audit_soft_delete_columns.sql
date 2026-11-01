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
