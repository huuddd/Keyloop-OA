-- Expand appointment status to support full lifecycle:
-- CONFIRMED -> IN_PROGRESS -> COMPLETED
--                          -> CANCELLED (from CONFIRMED or IN_PROGRESS)
-- CONFIRMED -> NO_SHOW

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
