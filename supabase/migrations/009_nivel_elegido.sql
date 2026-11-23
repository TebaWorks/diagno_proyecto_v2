-- Marca cuándo el usuario eligió su nivel (null = todavía no lo elige)
alter table public.perfil
  add column if not exists nivel_elegido_en timestamptz;

-- Las cuentas que ya existen ya pasaron por la pantalla de nivel
update public.perfil set nivel_elegido_en = now() where nivel_elegido_en is null;