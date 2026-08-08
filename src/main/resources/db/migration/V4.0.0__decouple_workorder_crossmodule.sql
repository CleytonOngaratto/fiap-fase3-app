-- D2 (hub / workorder): desacopla a work_order dos agregados customer, vehicle, auto_service e
-- parts_and_supply (preparação Fase 3 / bancos separados). As relações JPA cross-module
-- (@ManyToOne customer/vehicle, @ManyToMany services/parts) viram colunas denormalizadas + snapshot,
-- e as FKs cruzadas são removidas. A OS passa a guardar o nome+preço do serviço/peça DA ÉPOCA
-- (snapshot), sem depender de join no outro módulo. As colunas de referência (customer_id, vehicle_id,
-- auto_service_id, parts_and_supply_id) e as FKs same-module (-> work_orders) permanecem.

-- 1) work_orders: snapshot de email do cliente e placa do veículo -------------------------------
ALTER TABLE public.work_orders
    ADD COLUMN IF NOT EXISTS customer_email varchar(255),
    ADD COLUMN IF NOT EXISTS vehicle_plate  varchar(255);

UPDATE public.work_orders wo
SET customer_email = c.email
FROM public.customers c
WHERE wo.customer_id = c.id;

UPDATE public.work_orders wo
SET vehicle_plate = v.vehicleplate
FROM public.vehicles v
WHERE wo.vehicle_id = v.id;

ALTER TABLE public.work_orders
    DROP CONSTRAINT IF EXISTS fk_customer_work_orders,
    DROP CONSTRAINT IF EXISTS fk_vehicle_work_orders;

-- 2) work_order_services: snapshot de descrição e preço do serviço ------------------------------
ALTER TABLE public.work_order_services
    ADD COLUMN IF NOT EXISTS description varchar(255),
    ADD COLUMN IF NOT EXISTS price       numeric(38, 2);

UPDATE public.work_order_services wos
SET description = a.description,
    price       = a.price
FROM public.auto_service a
WHERE wos.auto_service_id = a.id;

ALTER TABLE public.work_order_services
    DROP CONSTRAINT IF EXISTS fk_auto_service_order_services;

-- 3) work_order_parts: snapshot dos dados da peça -----------------------------------------------
ALTER TABLE public.work_order_parts
    ADD COLUMN IF NOT EXISTS code         varchar(255),
    ADD COLUMN IF NOT EXISTS manufacturer varchar(255),
    ADD COLUMN IF NOT EXISTS description  varchar(255),
    ADD COLUMN IF NOT EXISTS price        numeric(38, 2),
    ADD COLUMN IF NOT EXISTS type         varchar(20),
    ADD COLUMN IF NOT EXISTS quantity     int4;

UPDATE public.work_order_parts wop
SET code         = p.code,
    manufacturer = p.manufacturer,
    description  = p.description,
    price        = p.price,
    type         = p.type,
    quantity     = p.quantity
FROM public.parts_and_supply p
WHERE wop.parts_and_supply_id = p.id;

ALTER TABLE public.work_order_parts
    DROP CONSTRAINT IF EXISTS fk_parts_work_order;
