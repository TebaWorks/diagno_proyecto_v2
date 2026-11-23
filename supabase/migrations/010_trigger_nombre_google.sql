-- El nombre de las cuentas de Google llega en full_name o name
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
    coalesce(
      nullif(trim(meta ->> 'nombre'), ''),
      nullif(trim(meta ->> 'full_name'), ''),
      nullif(trim(meta ->> 'name'), ''),
      split_part(new.email, '@', 1)
    ),
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