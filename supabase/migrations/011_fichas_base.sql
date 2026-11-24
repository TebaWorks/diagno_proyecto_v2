-- Quitar tildes (necesario para las palabras clave)
create extension if not exists unaccent with schema extensions;

-- Convierte un texto en palabras clave: minúsculas, sin tildes y sin palabras vacías
create or replace function public.palabras_clave_de(texto text)
returns text[]
language sql
stable
set search_path = public, extensions
as $$
  select coalesce(array_agg(distinct p), '{}')
  from regexp_split_to_table(lower(extensions.unaccent(coalesce(texto, ''))), '[^a-z0-9]+') as p
  where length(p) >= 3
    and p not in ('del', 'las', 'los', 'una', 'uno', 'por', 'para', 'con', 'sin', 'que',
                  'cada', 'como', 'mas', 'sus', 'esta', 'este', 'son', 'hay', 'debe', 'puede')
$$;

-- Genera las fichas base del modelo de referencia activo
create or replace function public.regenerar_fichas_base()
returns integer
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  total integer;
begin
  delete from public.conocimiento_offline where origen = 'base';

  -- 1) Testigos del tablero
  insert into public.conocimiento_offline
    (usuario_id, titulo, contenido, palabras_clave, etiqueta_imagen, categoria, urgencia, origen)
  select
    null,
    'Testigo: ' || t.nombre,
    left(
      t.significado || ' Qué hacer: ' || t.que_hacer ||
      case when t.puede_seguir_manejando
           then ' Puedes seguir manejando con precaución.'
           else ' No sigas manejando: detente en un lugar seguro y pide asistencia.' end,
      800),
    public.palabras_clave_de(t.nombre || ' testigo luz tablero ' || t.color),
    'testigo_' || array_to_string(public.palabras_clave_de(t.nombre), '_'),
    'testigo',
    t.urgencia,
    'base'
  from public.testigo t
  join public.modelo_vehiculo m on m.id = t.modelo_id and m.activo;

  -- 2) Pauta de mantención
  insert into public.conocimiento_offline
    (usuario_id, titulo, contenido, palabras_clave, categoria, urgencia, origen)
  select
    null,
    p.item,
    left(
      initcap(p.accion) || ' ' || lower(p.item) || ' ' ||
      concat_ws(' o ',
        case when p.intervalo_km is not null then 'cada ' || p.intervalo_km || ' km' end,
        case when p.intervalo_meses is not null then 'cada ' || p.intervalo_meses || ' meses' end) ||
      ', lo que ocurra primero.' || coalesce(' ' || p.descripcion, ''),
      800),
    public.palabras_clave_de(p.item || ' mantencion ' || p.categoria || ' ' || p.accion),
    p.categoria,
    'media',
    'base'
  from public.pauta_mantencion p
  join public.modelo_vehiculo m on m.id = p.modelo_id and m.activo;

  -- 3) Especificaciones, una ficha por categoría
  insert into public.conocimiento_offline
    (usuario_id, titulo, contenido, palabras_clave, categoria, urgencia, origen)
  select
    null,
    'Especificaciones: ' || e.categoria,
    left(e.texto || '.', 800),
    public.palabras_clave_de(e.datos || ' ' || e.categoria || ' especificacion medida dato'),
    e.categoria,
    'baja',
    'base'
  from (
    select esp.categoria,
           string_agg(esp.dato || ': ' || esp.valor || coalesce(' ' || esp.unidad, ''), '. ' order by esp.dato) as texto,
           string_agg(esp.dato, ' ') as datos
    from public.especificacion esp
    join public.modelo_vehiculo m on m.id = esp.modelo_id and m.activo
    group by esp.categoria
  ) e;

  select count(*) into total from public.conocimiento_offline where origen = 'base';
  return total;
end;
$$;

-- Solo se ejecuta desde el panel (administrador), nunca desde la app
revoke execute on function public.regenerar_fichas_base() from public, anon, authenticated;

-- Generar las fichas ahora
select public.regenerar_fichas_base() as fichas_base;