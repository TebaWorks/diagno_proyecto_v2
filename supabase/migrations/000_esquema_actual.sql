-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.

CREATE TABLE public.perfil (
  id uuid NOT NULL,
  nombre_completo text NOT NULL,
  nivel_conocimiento text NOT NULL DEFAULT 'novato'::text CHECK (nivel_conocimiento = ANY (ARRAY['novato'::text, 'con_conocimiento'::text, 'avanzado'::text])),
  creado_en timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT perfil_pkey PRIMARY KEY (id),
  CONSTRAINT perfil_id_fkey FOREIGN KEY (id) REFERENCES auth.users(id)
);
CREATE TABLE public.vehiculo (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  usuario_id uuid NOT NULL,
  tipo text NOT NULL CHECK (tipo = ANY (ARRAY['auto'::text, 'moto'::text])),
  marca text NOT NULL,
  modelo text NOT NULL,
  anio integer,
  kilometraje integer DEFAULT 0 CHECK (kilometraje >= 0),
  creado_en timestamp with time zone NOT NULL DEFAULT now(),
  modelo_id uuid,
  combustible text,
  CONSTRAINT vehiculo_pkey PRIMARY KEY (id),
  CONSTRAINT vehiculo_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES auth.users(id),
  CONSTRAINT vehiculo_modelo_id_fkey FOREIGN KEY (modelo_id) REFERENCES public.modelo_vehiculo(id)
);
CREATE TABLE public.conversacion (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  usuario_id uuid NOT NULL,
  vehiculo_id uuid,
  titulo text NOT NULL DEFAULT 'Nueva conversación'::text,
  activa boolean NOT NULL DEFAULT true,
  creado_en timestamp with time zone NOT NULL DEFAULT now(),
  actualizado_en timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT conversacion_pkey PRIMARY KEY (id),
  CONSTRAINT conversacion_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES auth.users(id),
  CONSTRAINT conversacion_vehiculo_id_fkey FOREIGN KEY (vehiculo_id) REFERENCES public.vehiculo(id)
);
CREATE TABLE public.mensaje (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  conversacion_id uuid NOT NULL,
  emisor text NOT NULL CHECK (emisor = ANY (ARRAY['usuario'::text, 'asistente'::text, 'sistema'::text])),
  contenido text NOT NULL,
  creado_en timestamp with time zone NOT NULL DEFAULT now(),
  metadatos jsonb NOT NULL DEFAULT '{}'::jsonb,
  CONSTRAINT mensaje_pkey PRIMARY KEY (id),
  CONSTRAINT mensaje_conversacion_id_fkey FOREIGN KEY (conversacion_id) REFERENCES public.conversacion(id)
);
CREATE TABLE public.catalogo_chequeo (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  nombre text NOT NULL,
  descripcion text,
  categoria text NOT NULL CHECK (categoria = ANY (ARRAY['motor'::text, 'frenos'::text, 'neumaticos'::text, 'bateria'::text, 'fluidos'::text, 'general'::text])),
  nivel_dificultad text NOT NULL DEFAULT 'basico'::text CHECK (nivel_dificultad = ANY (ARRAY['basico'::text, 'intermedio'::text, 'avanzado'::text])),
  activo boolean NOT NULL DEFAULT true,
  creado_en timestamp with time zone NOT NULL DEFAULT now(),
  tipo text NOT NULL DEFAULT 'preventivo'::text CHECK (tipo = ANY (ARRAY['preventivo'::text, 'correctivo'::text])),
  modelo_id uuid,
  CONSTRAINT catalogo_chequeo_pkey PRIMARY KEY (id),
  CONSTRAINT catalogo_chequeo_modelo_id_fkey FOREIGN KEY (modelo_id) REFERENCES public.modelo_vehiculo(id)
);
CREATE TABLE public.catalogo_tarea (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  chequeo_id uuid NOT NULL,
  nombre text NOT NULL,
  descripcion text,
  orden integer NOT NULL DEFAULT 1 CHECK (orden > 0),
  obligatoria boolean NOT NULL DEFAULT true,
  creado_en timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT catalogo_tarea_pkey PRIMARY KEY (id),
  CONSTRAINT catalogo_tarea_chequeo_id_fkey FOREIGN KEY (chequeo_id) REFERENCES public.catalogo_chequeo(id)
);
CREATE TABLE public.guia_paso (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  tarea_id uuid NOT NULL,
  numero_paso integer NOT NULL CHECK (numero_paso > 0),
  titulo text NOT NULL,
  instruccion text NOT NULL,
  advertencia text,
  requiere_foto boolean NOT NULL DEFAULT false,
  creado_en timestamp with time zone NOT NULL DEFAULT now(),
  valor_normal text,
  senal_alerta text,
  CONSTRAINT guia_paso_pkey PRIMARY KEY (id),
  CONSTRAINT guia_paso_tarea_id_fkey FOREIGN KEY (tarea_id) REFERENCES public.catalogo_tarea(id)
);
CREATE TABLE public.chequeo (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  usuario_id uuid NOT NULL,
  vehiculo_id uuid NOT NULL,
  catalogo_chequeo_id uuid,
  estado text NOT NULL DEFAULT 'pendiente'::text CHECK (estado = ANY (ARRAY['pendiente'::text, 'en_progreso'::text, 'completado'::text, 'cancelado'::text])),
  resultado text CHECK (resultado IS NULL OR (resultado = ANY (ARRAY['normal'::text, 'observacion'::text, 'problema'::text]))),
  observaciones text,
  iniciado_en timestamp with time zone NOT NULL DEFAULT now(),
  completado_en timestamp with time zone,
  creado_en timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT chequeo_pkey PRIMARY KEY (id),
  CONSTRAINT chequeo_catalogo_chequeo_id_fkey FOREIGN KEY (catalogo_chequeo_id) REFERENCES public.catalogo_chequeo(id),
  CONSTRAINT chequeo_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES auth.users(id),
  CONSTRAINT chequeo_vehiculo_id_fkey FOREIGN KEY (vehiculo_id) REFERENCES public.vehiculo(id)
);
CREATE TABLE public.retroalimentacion (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  usuario_id uuid NOT NULL,
  mensaje_id uuid,
  calificacion smallint NOT NULL CHECK (calificacion >= 1 AND calificacion <= 5),
  comentario text,
  util boolean,
  creado_en timestamp with time zone NOT NULL DEFAULT now(),
  chequeo_id uuid,
  CONSTRAINT retroalimentacion_pkey PRIMARY KEY (id),
  CONSTRAINT retroalimentacion_chequeo_id_fkey FOREIGN KEY (chequeo_id) REFERENCES public.chequeo(id),
  CONSTRAINT retroalimentacion_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES auth.users(id),
  CONSTRAINT retroalimentacion_mensaje_id_fkey FOREIGN KEY (mensaje_id) REFERENCES public.mensaje(id)
);
CREATE TABLE public.kb_documento (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  titulo text NOT NULL,
  fuente text,
  autor text,
  licencia text,
  categoria text NOT NULL,
  contenido_original text,
  estado text NOT NULL DEFAULT 'pendiente'::text CHECK (estado = ANY (ARRAY['pendiente'::text, 'procesando'::text, 'publicado'::text, 'error'::text])),
  creado_en timestamp with time zone NOT NULL DEFAULT now(),
  actualizado_en timestamp with time zone NOT NULL DEFAULT now(),
  modelo_id uuid,
  CONSTRAINT kb_documento_pkey PRIMARY KEY (id),
  CONSTRAINT kb_documento_modelo_id_fkey FOREIGN KEY (modelo_id) REFERENCES public.modelo_vehiculo(id)
);
CREATE TABLE public.modelo_vehiculo (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  marca text NOT NULL,
  modelo text NOT NULL,
  anio_desde integer NOT NULL,
  anio_hasta integer NOT NULL,
  motor text,
  activo boolean NOT NULL DEFAULT true,
  creado_en timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT modelo_vehiculo_pkey PRIMARY KEY (id)
);
CREATE TABLE public.pauta_mantencion (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  modelo_id uuid NOT NULL,
  item text NOT NULL,
  accion text NOT NULL CHECK (accion = ANY (ARRAY['revisar'::text, 'cambiar'::text])),
  categoria text NOT NULL CHECK (categoria = ANY (ARRAY['motor'::text, 'frenos'::text, 'neumaticos'::text, 'bateria'::text, 'fluidos'::text, 'general'::text])),
  intervalo_km integer CHECK (intervalo_km > 0),
  intervalo_meses integer CHECK (intervalo_meses > 0),
  descripcion text,
  creado_en timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT pauta_mantencion_pkey PRIMARY KEY (id),
  CONSTRAINT pauta_mantencion_modelo_id_fkey FOREIGN KEY (modelo_id) REFERENCES public.modelo_vehiculo(id)
);
CREATE TABLE public.testigo (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  modelo_id uuid NOT NULL,
  nombre text NOT NULL,
  color text NOT NULL CHECK (color = ANY (ARRAY['rojo'::text, 'amarillo'::text, 'verde'::text, 'azul'::text, 'blanco'::text])),
  significado text NOT NULL,
  urgencia text NOT NULL CHECK (urgencia = ANY (ARRAY['baja'::text, 'media'::text, 'alta'::text])),
  que_hacer text NOT NULL,
  puede_seguir_manejando boolean NOT NULL,
  imagen_path text,
  creado_en timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT testigo_pkey PRIMARY KEY (id),
  CONSTRAINT testigo_modelo_id_fkey FOREIGN KEY (modelo_id) REFERENCES public.modelo_vehiculo(id)
);
CREATE TABLE public.especificacion (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  modelo_id uuid NOT NULL,
  categoria text NOT NULL CHECK (categoria = ANY (ARRAY['motor'::text, 'frenos'::text, 'neumaticos'::text, 'bateria'::text, 'fluidos'::text, 'general'::text])),
  dato text NOT NULL,
  valor text NOT NULL,
  unidad text,
  creado_en timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT especificacion_pkey PRIMARY KEY (id),
  CONSTRAINT especificacion_modelo_id_fkey FOREIGN KEY (modelo_id) REFERENCES public.modelo_vehiculo(id)
);
CREATE TABLE public.registro_mantencion (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  usuario_id uuid NOT NULL,
  vehiculo_id uuid NOT NULL,
  pauta_id uuid,
  descripcion text NOT NULL,
  kilometraje integer NOT NULL CHECK (kilometraje >= 0),
  fecha date NOT NULL DEFAULT CURRENT_DATE,
  observaciones text,
  creado_en timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT registro_mantencion_pkey PRIMARY KEY (id),
  CONSTRAINT registro_mantencion_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES auth.users(id),
  CONSTRAINT registro_mantencion_vehiculo_id_fkey FOREIGN KEY (vehiculo_id) REFERENCES public.vehiculo(id),
  CONSTRAINT registro_mantencion_pauta_id_fkey FOREIGN KEY (pauta_id) REFERENCES public.pauta_mantencion(id)
);
CREATE TABLE public.consulta (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  usuario_id uuid NOT NULL DEFAULT auth.uid(),
  titulo text NOT NULL DEFAULT ''::text,
  resumen text,
  vehiculo text,
  mensajes jsonb NOT NULL DEFAULT '[]'::jsonb,
  creado_en timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT consulta_pkey PRIMARY KEY (id),
  CONSTRAINT consulta_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES auth.users(id)
);