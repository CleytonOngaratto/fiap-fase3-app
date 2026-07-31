-- DROP TABLE public.customers;
CREATE TABLE public.customers
(
    id       int8 NOT NULL,
    document varchar(255) NULL,
    email    varchar(255) NULL,
    name     varchar(255) NULL,
    number   varchar(255) NULL,
    rg       varchar(255) NULL,
    CONSTRAINT customers_pkey PRIMARY KEY (id)
);

CREATE SEQUENCE public.customers_seq
    AS int8
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE CACHE 1;

-- DROP TABLE public.vehicles;
CREATE TABLE public.vehicles
(
    id           int8 NOT NULL,
    manufacturer varchar(255) NULL,
    modelname    varchar(255) NULL,
    modelyear    int4 NULL,
    vehicleplate varchar(255) NULL,
    owner_id     int8 NULL,
    CONSTRAINT vehicles_pkey PRIMARY KEY (id)
);

CREATE SEQUENCE public.vehicles_seq
    AS int8
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE CACHE 1;

-- public.vehicles foreign keys
ALTER TABLE public.vehicles
    ADD CONSTRAINT fk_customer_vehicles FOREIGN KEY (owner_id) REFERENCES public.customers (id);

-- DROP TABLE public.auto_service;
CREATE TABLE public.auto_service
(
    id          int8 NOT NULL,
    description varchar(255) NULL,
    price       numeric(38, 2) NULL,
    CONSTRAINT auto_service_pkey PRIMARY KEY (id)
);

CREATE SEQUENCE public.auto_service_seq
    AS int8
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE CACHE 1;

-- DROP TABLE public.work_orders;
CREATE TABLE public.work_orders
(
    id                     int8 NOT NULL,
    creation_date          timestamp(6) NULL,
    end_date               timestamp(6) NULL,
    status                 varchar(255) NULL,
    customer_id            int8 NULL,
    vehicle_id             int8 NULL,
    diagnostic_description varchar(255) NULL,
    budget_value           numeric(38, 2) NULL,
    budget_approval_date   timestamp(6) NULL,
    CONSTRAINT work_orders_pkey PRIMARY KEY (id),
    CONSTRAINT fk_customer_work_orders FOREIGN KEY (customer_id) REFERENCES public.customers (id),
    CONSTRAINT fk_vehicle_work_orders FOREIGN KEY (vehicle_id) REFERENCES public.vehicles (id)
);

CREATE SEQUENCE public.work_orders_seq
    AS int8
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE CACHE 1;

-- DROP TABLE public.work_order_services;
CREATE TABLE public.work_order_services
(
    work_order_id   int8 NOT NULL,
    auto_service_id int8 NOT NULL,
    CONSTRAINT work_order_services_pkey PRIMARY KEY (work_order_id, auto_service_id),
    CONSTRAINT fk_work_order_order_services FOREIGN KEY (work_order_id) REFERENCES public.work_orders (id),
    CONSTRAINT fk_auto_service_order_services FOREIGN KEY (auto_service_id) REFERENCES public.auto_service (id)
);

-- DROP TABLE public.parts_and_supply;
CREATE TABLE public.parts_and_supply
(
    id           int8 NOT NULL,
    code         varchar(255) NULL,
    manufacturer varchar(255) NULL,
    description  varchar(255) NULL,
    price        numeric(38, 2) NULL,
    type         varchar(20) NULL,
    quantity     int4 NULL,
    CONSTRAINT parts_and_supply_pkey PRIMARY KEY (id)
);

CREATE SEQUENCE public.parts_and_supply_seq
    AS int8
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE CACHE 1;

-- DROP TABLE public.users;
CREATE TABLE public.users
(
    id       int8 NOT NULL,
    username varchar(255) NOT NULL UNIQUE,
    password varchar(255) NOT NULL,
    salt     varchar(255) NULL,
    CONSTRAINT users_pkey PRIMARY KEY (id)
);

CREATE SEQUENCE public.users_seq
    AS int8
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE CACHE 1;

-- Tabela para armazenar as roles dos usuários (relacionamento @ElementCollection)
CREATE TABLE public.userentity_roles
(
    userentity_id int8 NOT NULL,
    roles         varchar(255) NOT NULL,
    CONSTRAINT fk_user_roles FOREIGN KEY (userentity_id) REFERENCES public.users (id)
);

-- Join table linking work orders to parts and supplies
CREATE TABLE public.work_order_parts
(
    work_order_id        int8 NOT NULL,
    parts_and_supply_id  int8 NOT NULL,
    CONSTRAINT work_order_parts_pkey PRIMARY KEY (work_order_id, parts_and_supply_id),
    CONSTRAINT fk_work_order_parts FOREIGN KEY (work_order_id) REFERENCES public.work_orders (id),
    CONSTRAINT fk_parts_work_order FOREIGN KEY (parts_and_supply_id) REFERENCES public.parts_and_supply (id)
);

-- Insert customers table
INSERT INTO customers (id, name, document, rg, email, number)
VALUES (1, 'John Silva', '12345678901', 'MG123456789', 'john.silva@email.com', '11987654321');
INSERT INTO customers (id, name, document, rg, email, number)
VALUES (2, 'Maria Santos', '98765432100', 'SP987654321', 'maria.santos@email.com', '11876543210');
INSERT INTO customers (id, name, document, rg, email, number)
VALUES (3, 'Carlos Oliveira', '11122233344', 'RJ111222333', 'carlos.oliveira@email.com', '11765432109');

-- Insert vehicles table
INSERT INTO vehicles (id, vehiclePlate, manufacturer, modelName, modelYear, owner_id)
VALUES (1, 'ABC-1234', 'Toyota', 'Corolla', 2022, 1);
INSERT INTO vehicles (id, vehiclePlate, manufacturer, modelName, modelYear, owner_id)
VALUES (2, 'XYZ-5678', 'Honda', 'Civic', 2021, 2);
INSERT INTO vehicles (id, vehiclePlate, manufacturer, modelName, modelYear, owner_id)
VALUES (3, 'DEF9G12', 'Volkswagen', 'Golf', 2023, 1);

-- Insert auto_service table
INSERT INTO public.auto_service (id, description, price)
VALUES (1, 'Troca de Óleo do Motor', 150.00);
INSERT INTO public.auto_service (id, description, price)
VALUES (2, 'Alinhamento e Balanceamento', 120.50);
INSERT INTO public.auto_service (id, description, price)
VALUES (3, 'Troca de Pneus (unidade)', 80.00);
INSERT INTO public.auto_service (id, description, price)
VALUES (4, 'Revisão do Sistema de Freios', 250.00);
INSERT INTO public.auto_service (id, description, price)
VALUES (5, 'Troca de Filtro de Ar', 60.75);
INSERT INTO public.auto_service (id, description, price)
VALUES (6, 'Limpeza do Sistema de Injeção', 300.00);
INSERT INTO public.auto_service (id, description, price)
VALUES (7, 'Troca da Correia Dentada', 450.00);
INSERT INTO public.auto_service (id, description, price)
VALUES (8, 'Recarga do Ar Condicionado', 180.00);
INSERT INTO public.auto_service (id, description, price)
VALUES (9, 'Diagnóstico Eletrônico com Scanner', 100.00);
INSERT INTO public.auto_service (id, description, price)
VALUES (10, 'Higienização do Ar Condicionado', 90.00);

INSERT INTO parts_and_supply (id, code, manufacturer, description, price, type, quantity)
VALUES (1, 'P001', 'Bosch', 'Filtro de óleo', 35.90, 'UNITARY', 10);
INSERT INTO parts_and_supply (id, code, manufacturer, description, price, type, quantity)
VALUES (2, 'P002', 'Valeo', 'Pastilha de freio', 120.00, 'UNITARY', 5);
INSERT INTO parts_and_supply (id, code, manufacturer, description, price, type, quantity)
VALUES (3, 'P003', 'NGK', 'Vela de ignição', 25.50, 'PACKAGE', 20);
INSERT INTO parts_and_supply (id, code, manufacturer, description, price, type, quantity)
VALUES (4, 'P004', 'Mahle', 'Filtro de ar', 40.00, 'UNITARY', 4);
INSERT INTO parts_and_supply (id, code, manufacturer, description, price, type, quantity)
VALUES (5, 'P005', 'Monroe', 'Amortecedor dianteiro', 250.00, 'UNITARY', 4);
INSERT INTO parts_and_supply (id, code, manufacturer, description, price, type, quantity)
VALUES (6, 'S001', 'Shell', 'Óleo sintético 5W30', 150.00, 'UNITARY', 20);
INSERT INTO parts_and_supply (id, code, manufacturer, description, price, type, quantity)
VALUES (7, 'S002', 'Mobil', 'Óleo mineral 20W50', 90.00, 'UNITARY', 20);
INSERT INTO parts_and_supply (id, code, manufacturer, description, price, type, quantity)
VALUES (8, 'S003', 'Texaco', 'Aditivo radiador', 30.00, 'UNITARY', 30);
INSERT INTO parts_and_supply (id, code, manufacturer, description, price, type, quantity)
VALUES (9, 'S004', 'Bardahl', 'Desengripante', 18.00, 'UNITARY', 20);
INSERT INTO parts_and_supply (id, code, manufacturer, description, price, type, quantity)
VALUES (10, 'S005', '3M', 'Silicone spray', 22.00, 'UNITARY', 10);

-- Restart sequences
ALTER SEQUENCE customers_seq RESTART WITH 4;
ALTER SEQUENCE vehicles_seq RESTART WITH 4;
ALTER SEQUENCE auto_service_seq RESTART WITH 11;
ALTER SEQUENCE parts_and_supply_seq RESTART WITH 11;
ALTER SEQUENCE work_orders_seq RESTART WITH 1;
