ALTER TABLE public.work_orders
    ADD COLUMN IF NOT EXISTS deleted boolean NOT NULL DEFAULT false;
