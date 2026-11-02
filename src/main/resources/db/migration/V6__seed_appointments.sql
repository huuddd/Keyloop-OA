-- Sample appointments for manual testing / demo
-- Uses seed data from V2: dealership 1, bays 1-3, technicians 1-5, customers 1-2, vehicles 1-2

-- 1. CONFIRMED appointment (tomorrow 09:00, Oil Change 60min, Bay A, Alex)
INSERT INTO appointments (id, customer_id, vehicle_id, service_bay_id, technician_id, service_type_id,
                          start_time, end_time, status, created_at, updated_at)
VALUES ('a0000001-0000-0000-0000-000000000001', 1, 1, 1, 1, 1,
        CURRENT_DATE + INTERVAL '1 day' + TIME '09:00', CURRENT_DATE + INTERVAL '1 day' + TIME '10:00',
        'CONFIRMED', NOW(), NOW());

-- 2. CONFIRMED appointment (tomorrow 10:00, Brake Replacement 90min, Bay B, Sarah - BRAKES specialist)
INSERT INTO appointments (id, customer_id, vehicle_id, service_bay_id, technician_id, service_type_id,
                          start_time, end_time, status, created_at, updated_at)
VALUES ('a0000002-0000-0000-0000-000000000002', 2, 2, 2, 4, 3,
        CURRENT_DATE + INTERVAL '1 day' + TIME '10:00', CURRENT_DATE + INTERVAL '1 day' + TIME '11:30',
        'CONFIRMED', NOW(), NOW());

-- 3. IN_PROGRESS appointment (today 14:00, Full Inspection 120min, Bay C, Maria)
INSERT INTO appointments (id, customer_id, vehicle_id, service_bay_id, technician_id, service_type_id,
                          start_time, end_time, status, created_at, updated_at)
VALUES ('a0000003-0000-0000-0000-000000000003', 1, 1, 3, 2, 2,
        CURRENT_DATE + TIME '14:00', CURRENT_DATE + TIME '16:00',
        'IN_PROGRESS', NOW(), NOW());

-- 4. COMPLETED appointment (yesterday, Engine Diagnostic 60min, Bay A, Maria - ENGINE specialist)
INSERT INTO appointments (id, customer_id, vehicle_id, service_bay_id, technician_id, service_type_id,
                          start_time, end_time, status, created_at, updated_at)
VALUES ('a0000004-0000-0000-0000-000000000004', 2, 2, 1, 2, 4,
        CURRENT_DATE - INTERVAL '1 day' + TIME '09:00', CURRENT_DATE - INTERVAL '1 day' + TIME '10:00',
        'COMPLETED', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

-- 5. CANCELLED appointment (tomorrow 11:00, Oil Change, Bay A, James)
INSERT INTO appointments (id, customer_id, vehicle_id, service_bay_id, technician_id, service_type_id,
                          start_time, end_time, status, created_at, updated_at)
VALUES ('a0000005-0000-0000-0000-000000000005', 1, 1, 1, 3, 1,
        CURRENT_DATE + INTERVAL '1 day' + TIME '11:00', CURRENT_DATE + INTERVAL '1 day' + TIME '12:00',
        'CANCELLED', NOW(), NOW());

-- 6. NO_SHOW appointment (yesterday 15:00, Oil Change, Bay B, David)
INSERT INTO appointments (id, customer_id, vehicle_id, service_bay_id, technician_id, service_type_id,
                          start_time, end_time, status, created_at, updated_at)
VALUES ('a0000006-0000-0000-0000-000000000006', 2, 2, 2, 5, 1,
        CURRENT_DATE - INTERVAL '1 day' + TIME '15:00', CURRENT_DATE - INTERVAL '1 day' + TIME '16:00',
        'NO_SHOW', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');
