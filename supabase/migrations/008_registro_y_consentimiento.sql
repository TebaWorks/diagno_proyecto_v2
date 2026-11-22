-- 1. Columnas para registrar el consentimiento (Ley 21.719)
alter table public.perfil
  add column if not exists acepta_privacidad boolean not null default false,
  add column if not exists acepta_compartir boolean not null default false,
  add column if not exists version_politica text,
  add column if not exists consentimiento_en timestamptz;

-- 2. Trigger: crea el perfil junto con la cuenta, en la misma transacción
create or replace function public.crear_perfil()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
declare
  meta jsonb := coalesce(new.raw_user_meta_data, '{}'::jsonb);
begin
  insert into public.perfil (
    id, nombre_completo, nivel_conocimiento,
    acepta_privacidad, acepta_compartir, version_politica, consentimiento_en
  )
  values (
    new.id,
    coalesce(nullif(trim(meta ->> 'nombre'), ''), split_part(new.email, '@', 1)),
    case when meta ->> 'nivel' in ('novato', 'con_conocimiento', 'avanzado')
         then meta ->> 'nivel' else 'novato' end,
    coalesce((meta ->> 'acepta_privacidad')::boolean, false),
    coalesce((meta ->> 'acepta_compartir')::boolean, false),
    meta ->> 'version_politica',
    case when coalesce((meta ->> 'acepta_privacidad')::boolean, false) then now() else null end
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists al_registrar_usuario on auth.users;
create trigger al_registrar_usuario
  after insert on auth.users
  for each row execute function public.crear_perfil();

-- 3. Reparar las cuentas que quedaron sin perfil (como la tuya)
insert into public.perfil (id, nombre_completo)
select u.id, coalesce(nullif(trim(u.raw_user_meta_data ->> 'nombre'), ''), split_part(u.email, '@', 1))
from auth.users u
left join public.perfil p on p.id = u.id
where p.id is null;