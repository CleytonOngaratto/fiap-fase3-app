INSERT INTO auto_service(id, description, price)
VALUES (1, 'Troca de Óleo', 299.99);

INSERT INTO auto_service(id, description, price)
VALUES (2, 'Alinhamento', 199.99);

ALTER SEQUENCE auto_service_seq RESTART WITH 3;

INSERT INTO public.parts_and_supply(id, code, manufacturer, description, price, type, quantity)
VALUES (1, 'P001', 'Bosch', 'Filtro de óleo', 35.90, 'UNITARY', 10);
INSERT INTO public.parts_and_supply(id, code, manufacturer, description, price, type, quantity)
VALUES (2, 'P002', 'Valeo', 'Pastilha de freio', 120.00, 'UNITARY', 5);
INSERT INTO public.parts_and_supply(id, code, manufacturer, description, price, type, quantity)
VALUES (3, 'P003', 'Mahle', 'Filtro de ar', 40.00, 'UNITARY', 4);

ALTER SEQUENCE auto_service_SEQ RESTART WITH 3;
ALTER SEQUENCE parts_and_supply_SEQ RESTART WITH 4;

-- Customers are required so vehicle creation (which now validates an existing owner, D2/D3)
-- can reference customerId = 1..3. Mirrors the production seed in V1.0.0__oficina.sql.
INSERT INTO customers (id, name, document, rg, email, number)
VALUES (1, 'John Silva', '12345678901', 'MG123456789', 'john.silva@email.com', '11987654321');
INSERT INTO customers (id, name, document, rg, email, number)
VALUES (2, 'Maria Santos', '98765432100', 'SP987654321', 'maria.santos@email.com', '11876543210');
INSERT INTO customers (id, name, document, rg, email, number)
VALUES (3, 'Carlos Oliveira', '11122233344', 'RJ111222333', 'carlos.oliveira@email.com', '11765432109');

ALTER SEQUENCE customers_seq RESTART WITH 4;

INSERT INTO vehicles (id, vehiclePlate, manufacturer, modelName, modelYear, owner_id)
VALUES (1, 'ABC-1234', 'Toyota', 'Corolla', 2020, 1);

INSERT INTO vehicles (id, vehiclePlate, manufacturer, modelName, modelYear, owner_id)
VALUES (2, 'DEF-5678', 'Honda', 'Civic', 2021, 2);

INSERT INTO vehicles (id, vehiclePlate, manufacturer, modelName, modelYear, owner_id)
VALUES (3, 'GHI-9012', 'Honda', 'Fit', 2019, 1);

ALTER SEQUENCE vehicles_seq RESTART WITH 4;
