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
