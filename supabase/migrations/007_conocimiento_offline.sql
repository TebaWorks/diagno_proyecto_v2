create table if not exists public.conocimiento_offline (
  id uuid primary key default gen_random_uuid(),
  usuario_id uuid references auth.users(id) on delete cascade,
  titulo text not null,
  contenido text not null check (char_length(contenido) <= 800),
  palabras_clave text[] not null default '{}',
  etiqueta_imagen text,
  categoria text,
  urgencia text not null default 'media' check (urgencia in ('baja', 'media', 'alta')),
  origen text not null default 'consulta' check (origen in ('base', 'consulta')),
  comunitaria boolean not null default false,
  creado_en timestamptz not null default now()
);

alter table public.conocimiento_offline enable row level security;

drop policy if exists "conocimiento_offline_select" on public.conocimiento_offline;
create policy "conocimiento_offline_select" on public.conocimiento_offline
  for select to authenticated
  using (usuario_id = (select auth.uid()) or usuario_id is null or comunitaria);

create index if not exists conocimiento_offline_palabras_idx
  on public.conocimiento_offline using gin (palabras_clave);