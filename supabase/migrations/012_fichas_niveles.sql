alter table public.conocimiento_offline
  add column if not exists preguntas text[] not null default '{}',
  add column if not exists respuesta_novato text,
  add column if not exists respuesta_intermedio text,
  add column if not exists respuesta_avanzado text,
  add column if not exists pasos text[] not null default '{}',
  add column if not exists recomendaciones text[] not null default '{}';