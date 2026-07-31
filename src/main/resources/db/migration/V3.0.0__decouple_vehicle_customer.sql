-- D2: desacopla vehicle de customer (preparação Fase 3 / microsserviços).
-- A relação some do JPA (@ManyToOne -> Long customerId, ainda armazenado na coluna owner_id).
-- Remover a FK cruzada para que os agregados possam viver em bancos separados no futuro.
-- A regra "veículo sempre tem dono" passa a ser imposta na aplicação (VehicleInteractor +
-- CustomerExistencePort), não pelo banco. A coluna owner_id permanece como referência lógica.
ALTER TABLE public.vehicles
    DROP CONSTRAINT IF EXISTS fk_customer_vehicles;
