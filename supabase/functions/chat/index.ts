import { createClient } from "npm:@supabase/supabase-js@2";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const MAX_HISTORIAL = 10;

const INSTRUCCIONES_BASE = `
Eres Diagno, un asistente de mantención automotriz PREVENTIVA.
Reglas:
- Usa como fuente principal la INFORMACIÓN DEL VEHÍCULO que viene más abajo. No inventes datos técnicos (medidas, intervalos, capacidades) que no estén ahí; si falta un dato, dilo con claridad.
- Ayudas a anticipar problemas con chequeos simples y la pauta de mantención. Orientas, no diagnosticas fallas complejas.
- Solo puedes dar guías paso a paso para: cambio de rueda de repuesto, filtro de aire y filtro de habitáculo.
- Ante cualquier caso riesgoso o fuera de esas tareas, recomienda acudir a un taller mecánico.
- Si algo compromete la seguridad (frenos, dirección, temperatura alta, presión de aceite, testigos rojos), indica claramente que no siga manejando y busque ayuda.
- Si el usuario envía una foto, describe lo que ves con prudencia y aclara cuando no puedas asegurar algo.
- Responde en español de Chile, de forma clara y breve.

FORMATO DE SALIDA (JSON obligatorio):
- "respuesta": el texto para el usuario (puedes usar listas con guiones).
- "necesita_busqueda": true SOLO si la pregunta es automotriz, la INFORMACIÓN DEL VEHÍCULO no alcanza para responder bien y una búsqueda en internet aportaría. En cualquier otro caso, false. Ante una emergencia de seguridad, siempre false.
- "consulta_busqueda": si necesita_busqueda es true, una búsqueda breve en español que incluya la marca y el modelo del vehículo; si no, texto vacío.
- "derivar_taller": true si recomiendas acudir a un taller mecánico.
- "resumen_offline": la respuesta resumida en máximo 400 caracteres, autosuficiente y en lenguaje simple, para consultarla sin internet. Texto vacío si la pregunta no es automotriz.
- "palabras_clave": de 3 a 8 palabras clave en minúsculas y sin tildes (incluye sinónimos comunes, por ejemplo "neumatico", "llanta"). Lista vacía si la pregunta no es automotriz.
`;

const INSTRUCCIONES_WEB = `
USO DE LOS RESULTADOS WEB:
- Son solo un APOYO. Si contradicen la INFORMACIÓN DEL VEHÍCULO, manda la información del vehículo.
- Usa solo información coherente y prudente; descarta lo dudoso.
- Aclara en la respuesta qué parte proviene de internet.
- Aunque la web lo explique, NO entregues guías paso a paso fuera de las tareas permitidas: deriva a un taller.
- En esta respuesta, "necesita_busqueda" debe ser false.
`;

const INSTRUCCIONES_ANALISIS = `
Eres Diagno, un asistente de mantención automotriz PREVENTIVA. Con base en la conversación (y la foto, si la hay), entrega el RESULTADO DEL ANÁLISIS.
Reglas:
- "titulo": máximo 10 palabras, por ejemplo "Posible desgaste de pastillas de freno".
- "confianza": entero de 0 a 100. Si falta información, usa una confianza baja.
- "descripcion": 1 o 2 frases que expliquen el porqué.
- "recomendaciones": de 3 a 4 acciones cortas.
- "pasos": de 4 a 6 pasos ordenados, cada uno con 2 a 4 "acciones" cortas. Solo revisiones simples que el usuario pueda hacer sin herramientas especiales, o las guías permitidas (rueda de repuesto, filtro de aire, filtro de habitáculo). Cualquier reparación mayor se reemplaza por un paso "Acudir a un taller mecánico".
- Si hay riesgo para la seguridad, el PRIMER paso es detener el vehículo en un lugar seguro y pedir asistencia.
- "imagen": una de estas claves: bujias, bateria, frenos, neumaticos, aceite, filtro, luces, motor, general.
- "derivar_taller": true si recomiendas acudir a un taller.
- Usa la INFORMACIÓN DEL VEHÍCULO como fuente principal; no inventes datos técnicos.
- Responde en español de Chile.
`;

const INSTRUCCIONES_NIVEL: Record<string, string> = {
  novato: "El usuario es NOVATO: usa palabras simples, explica cada término técnico y da pasos muy concretos.",
  con_conocimiento: "El usuario tiene conocimientos básicos: puedes usar términos comunes sin explicar lo elemental.",
  avanzado: "El usuario tiene conocimientos avanzados: puedes ser técnico y directo.",
};

const ESQUEMA = {
  type: "OBJECT",
  properties: {
    respuesta: { type: "STRING" },
    necesita_busqueda: { type: "BOOLEAN" },
    consulta_busqueda: { type: "STRING" },
    derivar_taller: { type: "BOOLEAN" },
    resumen_offline: { type: "STRING" },
    palabras_clave: { type: "ARRAY", items: { type: "STRING" } },
  },
  required: ["respuesta", "necesita_busqueda", "derivar_taller"],
};

// Minúsculas, sin tildes y sin espacios sobrantes
const normalizar = (s: string) => s.normalize("NFD").replace(/\p{M}/gu, "").toLowerCase().trim();

const ESQUEMA_ANALISIS = {
  type: "OBJECT",
  properties: {
    titulo: { type: "STRING" },
    confianza: { type: "INTEGER" },
    descripcion: { type: "STRING" },
    recomendaciones: { type: "ARRAY", items: { type: "STRING" } },
    pasos: {
      type: "ARRAY",
      items: {
        type: "OBJECT",
        properties: {
          titulo: { type: "STRING" },
          detalle: { type: "STRING" },
          imagen: { type: "STRING", enum: ["bujias", "bateria", "frenos", "neumaticos", "aceite", "filtro", "luces", "motor", "general"] },
          acciones: { type: "ARRAY", items: { type: "STRING" } },
        },
        required: ["titulo", "detalle", "imagen", "acciones"],
      },
    },
    derivar_taller: { type: "BOOLEAN" },
  },
  required: ["titulo", "confianza", "descripcion", "recomendaciones", "pasos", "derivar_taller"],
};

function json(cuerpo: unknown, status = 200) {
  return new Response(JSON.stringify(cuerpo), {
    status,
    headers: { ...cors, "Content-Type": "application/json; charset=utf-8" },
  });
}

// Calcula si cada ítem de la pauta está al día, próximo o vencido
function calcularEstado(item: any, kmActual: number, ultimo: any): string {
  const kmBase = ultimo?.kilometraje ?? 0;
  const kmDesde = kmActual - kmBase;
  const partes: string[] = [];
  let estado = "al día";

  if (item.intervalo_km) {
    const proporcion = kmDesde / item.intervalo_km;
    if (proporcion >= 1) estado = "VENCIDO";
    else if (proporcion >= 0.8) estado = "PRÓXIMO";
    partes.push(`${kmDesde} km de ${item.intervalo_km} km`);
  }
  if (item.intervalo_meses && ultimo?.fecha) {
    const meses = (Date.now() - new Date(ultimo.fecha).getTime()) / (1000 * 60 * 60 * 24 * 30.4);
    const proporcion = meses / item.intervalo_meses;
    if (proporcion >= 1) estado = "VENCIDO";
    else if (proporcion >= 0.8 && estado === "al día") estado = "PRÓXIMO";
    partes.push(`${Math.floor(meses)} de ${item.intervalo_meses} meses`);
  }
  const origen = ultimo
    ? `última vez a los ${ultimo.kilometraje} km (${ultimo.fecha})`
    : "sin registro (se asume desde 0 km)";
  return `- ${item.item} [${item.accion}]: ${estado} — ${partes.join(", ")}; ${origen}`;
}

// Junta la información del vehículo del usuario y del modelo de referencia en un texto para Gemini
async function cargarConocimiento(sb: any, vehiculo: any): Promise<{ texto: string; nombre: string }> {
  // Si el vehículo no tiene modelo asociado, se usa el modelo de referencia activo del prototipo
  let m = vehiculo?.modelo_id ?? null;
  if (!m) {
    const { data: mv } = await sb.from("modelo_vehiculo").select("id").eq("activo", true).limit(1).maybeSingle();
    m = mv?.id ?? null;
  }
  if (!m) {
    return { texto: "No hay información técnica de referencia cargada. Responde de forma general y prudente.", nombre: "" };
  }

  const [modelo, pauta, testigos, specs, fichas, registros] = await Promise.all([
    sb.from("modelo_vehiculo").select("marca, modelo, anio_desde, anio_hasta, motor").eq("id", m).maybeSingle(),
    sb.from("pauta_mantencion").select("id, item, accion, categoria, intervalo_km, intervalo_meses").eq("modelo_id", m),
    sb.from("testigo").select("nombre, color, significado, urgencia, que_hacer, puede_seguir_manejando").eq("modelo_id", m),
    sb.from("especificacion").select("dato, valor, unidad").eq("modelo_id", m),
    sb.from("kb_documento").select("titulo, contenido_original").or(`modelo_id.eq.${m},modelo_id.is.null`),
    vehiculo?.id
      ? sb.from("registro_mantencion").select("pauta_id, kilometraje, fecha").eq("vehiculo_id", vehiculo.id).order("fecha", { ascending: false })
      : Promise.resolve({ data: [] }),
  ]);

  const ultimoPorItem = new Map<string, any>();
  for (const r of registros.data ?? []) {
    if (r.pauta_id && !ultimoPorItem.has(r.pauta_id)) ultimoPorItem.set(r.pauta_id, r);
  }

  const km = vehiculo?.kilometraje ?? 0;
  const md = modelo.data;
  const delUsuario = vehiculo
    ? `VEHÍCULO DEL USUARIO: ${vehiculo.marca ?? ""} ${vehiculo.modelo ?? ""} ${vehiculo.anio ?? ""}${vehiculo.combustible ? ", " + vehiculo.combustible : ""}. Kilometraje actual: ${km} km.`
    : "El usuario todavía no registra un vehículo; pídele que lo registre para personalizar la mantención.";
  const nombre = `${vehiculo?.marca ?? md?.marca ?? ""} ${vehiculo?.modelo ?? md?.modelo ?? ""}`.trim();

  const texto = [
    delUsuario,
    `MODELO DE REFERENCIA PARA LOS DATOS TÉCNICOS: ${md?.marca} ${md?.modelo} (${md?.anio_desde}–${md?.anio_hasta}), motor ${md?.motor ?? "no indicado"}.`,
    `\nESTADO DE MANTENCIÓN (calculado por el sistema):`,
    ...(pauta.data ?? []).map((p: any) => calcularEstado(p, km, ultimoPorItem.get(p.id))),
    `\nESPECIFICACIONES:`,
    ...(specs.data ?? []).map((s: any) => `- ${s.dato}: ${s.valor}${s.unidad ? " " + s.unidad : ""}`),
    `\nTESTIGOS DEL TABLERO:`,
    ...(testigos.data ?? []).map((t: any) =>
      `- ${t.nombre} (${t.color}, urgencia ${t.urgencia}): ${t.significado} Qué hacer: ${t.que_hacer} ¿Puede seguir manejando? ${t.puede_seguir_manejando ? "Sí, con precaución" : "NO"}.`),
    `\nFICHAS DE CONOCIMIENTO:`,
    ...(fichas.data ?? []).map((f: any) => `### ${f.titulo}\n${f.contenido_original ?? ""}`),
  ].join("\n");
  return { texto, nombre };
}

function construirContenido(historial: any[], mensaje: string, imagen: any) {
  const contents = historial.map((h) => ({
    role: h.emisor === "usuario" ? "user" : "model",
    parts: [{ text: h.contenido }],
  }));
  const partes: any[] = [{ text: mensaje }];
  if (imagen) partes.push({ inline_data: { mime_type: imagen.mime, data: imagen.base64 } });
  contents.push({ role: "user", parts: partes });
  return contents;
}

// Llama a Gemini pidiendo una respuesta JSON con el esquema indicado
async function pedirJSON(sistema: string, contents: any[], esquema: any) {
  const modelo = Deno.env.get("GEMINI_MODEL") ?? "gemini-3.5-flash-lite";
  const r = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${modelo}:generateContent`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "x-goog-api-key": Deno.env.get("GEMINI_API_KEY")! },
    body: JSON.stringify({
      system_instruction: { parts: [{ text: sistema }] },
      contents,
      generationConfig: { responseMimeType: "application/json", responseSchema: esquema },
    }),
  });

  if (r.status === 429) throw new Error("CUOTA");
  if (!r.ok) {
    console.error("Error de Gemini:", r.status, await r.text());
    throw new Error("GEMINI_ERROR");
  }
  const datos = await r.json();
  const texto = datos.candidates?.[0]?.content?.parts?.map((p: any) => p.text ?? "").join("") ?? "";
  try {
    return { modelo, datos: JSON.parse(texto), texto };
  } catch {
    return { modelo, datos: null, texto };
  }
}

async function preguntarGemini(sistema: string, contents: any[]) {
  const { modelo, datos: o, texto } = await pedirJSON(sistema, contents, ESQUEMA);
  if (!o) {
    return {
      modelo, respuesta: texto || "No pude generar una respuesta.", necesita_busqueda: false,
      consulta_busqueda: "", derivar_taller: false, resumen_offline: "", palabras_clave: [] as string[],
    };
  }
  return {
    modelo,
    respuesta: String(o.respuesta ?? "No pude generar una respuesta."),
    necesita_busqueda: o.necesita_busqueda === true,
    consulta_busqueda: String(o.consulta_busqueda ?? ""),
    derivar_taller: o.derivar_taller === true,
    resumen_offline: String(o.resumen_offline ?? "").slice(0, 800),
    palabras_clave: Array.isArray(o.palabras_clave)
      ? [...new Set(o.palabras_clave.map((p: any) => normalizar(String(p))).filter(Boolean))].slice(0, 8) as string[]
      : [] as string[],
  };
}

async function buscarWeb(consulta: string) {
  const r = await fetch("https://api.tavily.com/search", {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${Deno.env.get("TAVILY_API_KEY")}` },
    body: JSON.stringify({ query: consulta, search_depth: "basic", max_results: 3 }),
  });
  if (!r.ok) {
    console.error("Error de Tavily:", r.status, await r.text());
    return [];
  }
  const d = await r.json();
  return (d.results ?? []).map((x: any) => ({
    titulo: x.title ?? "",
    url: x.url ?? "",
    contenido: String(x.content ?? "").slice(0, 1200),
  }));
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });

  try {
    const url = Deno.env.get("SUPABASE_URL")!;
    // Cliente con los permisos del usuario (respeta RLS)
    const sb = createClient(url, Deno.env.get("SUPABASE_ANON_KEY")!, {
      global: { headers: { Authorization: req.headers.get("Authorization") ?? "" } },
    });
    // Cliente de servicio: solo para guardar la respuesta de la IA y las fichas offline
    const admin = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);

    const { data: { user } } = await sb.auth.getUser();
    if (!user) return json({ error: "No autenticado" }, 401);

    const body = await req.json();
    const { mensaje, modo, imagen_base64, imagen_mime, buscar_web, tipo } = body;
    let { conversacion_id, vehiculo_id } = body;
    if (!mensaje || typeof mensaje !== "string") return json({ error: "Falta el mensaje" }, 400);
    const esAnalisis = tipo === "analisis";

    // Límites de uso (protección contra abuso y consumo excesivo de cuota)
    if (mensaje.length > 2000) {
      return json({ error: "El mensaje es demasiado largo (máximo 2.000 caracteres)." }, 413);
    }
    if (imagen_base64 && imagen_base64.length > 2_000_000) {
      return json({ error: "La imagen es demasiado grande (máximo 1,5 MB)." }, 413);
    }
    const haceUnMinuto = new Date(Date.now() - 60_000).toISOString();
    const { count } = await sb.from("mensaje")
      .select("id", { count: "exact", head: true })
      .eq("emisor", "usuario")
      .gte("creado_en", haceUnMinuto);
    if ((count ?? 0) >= 10) {
      return json({ error: "Demasiadas consultas seguidas. Espera un minuto." }, 429);
    }

    if (modo === "mock") {
      return json({
        conversacion_id: conversacion_id ?? null,
        respuesta: `[Modo simulado] Recibí: "${mensaje}".`,
        derivar_taller: false, uso_web: false, fuentes: [], modo: "mock",
      });
    }
    if (esAnalisis && !conversacion_id) {
      return json({ error: "Primero conversa con el asistente para poder analizar." }, 400);
    }

    // 1. Perfil y nivel
    const { data: perfil } = await sb.from("perfil").select("nivel_conocimiento").eq("id", user.id).maybeSingle();
    const nivel = perfil?.nivel_conocimiento ?? "novato";
    const textoNivel = INSTRUCCIONES_NIVEL[nivel] ?? INSTRUCCIONES_NIVEL.novato;

    // 2. Vehículo (el indicado o el más reciente)
    let q = sb.from("vehiculo").select("id, marca, modelo, anio, combustible, kilometraje, modelo_id").eq("usuario_id", user.id);
    q = vehiculo_id ? q.eq("id", vehiculo_id) : q.order("creado_en", { ascending: false }).limit(1);
    const { data: vehiculos } = await q;
    const vehiculo = vehiculos?.[0] ?? null;

    // 3. Conocimiento del vehículo
    const conocimiento = await cargarConocimiento(sb, vehiculo);

    // 4. Conversación: crear o verificar que sea del usuario
    if (!conversacion_id) {
      const { data: c, error } = await sb.from("conversacion")
        .insert({ usuario_id: user.id, vehiculo_id: vehiculo?.id ?? null, titulo: mensaje.slice(0, 60) })
        .select("id").single();
      if (error) throw error;
      conversacion_id = c.id;
    } else {
      const { data: c } = await sb.from("conversacion").select("id").eq("id", conversacion_id).maybeSingle();
      if (!c) return json({ error: "Conversación no encontrada" }, 404);
    }

    // 5. Historial reciente
    const { data: hist } = await sb.from("mensaje").select("emisor, contenido")
      .eq("conversacion_id", conversacion_id).in("emisor", ["usuario", "asistente"])
      .order("creado_en", { ascending: false }).limit(MAX_HISTORIAL);
    const historial = (hist ?? []).reverse();
    const imagen = imagen_base64 ? { base64: imagen_base64, mime: imagen_mime ?? "image/jpeg" } : null;

    // 6. Modo análisis: devuelve el resultado estructurado y no guarda nada
    if (esAnalisis) {
      const sistemaA = INSTRUCCIONES_ANALISIS + "\n" + textoNivel + "\n\n=== INFORMACIÓN DEL VEHÍCULO ===\n" + conocimiento.texto;
      const contentsA = construirContenido(historial, "Entrega el resultado del análisis de esta conversación en el formato indicado.", imagen);
      const { datos } = await pedirJSON(sistemaA, contentsA, ESQUEMA_ANALISIS);
      if (!datos) return json({ error: "La IA no devolvió un análisis válido." }, 502);
      return json({ conversacion_id, analisis: datos });
    }

    // 7. Guardar el mensaje del usuario (con los permisos del propio usuario)
    const { error: errorUsuario } = await sb.from("mensaje").insert({
      conversacion_id, emisor: "usuario", contenido: mensaje, metadatos: { con_imagen: !!imagen_base64 },
    });
    if (errorUsuario) console.error("No se guardó el mensaje del usuario:", errorUsuario);

    // 8. Primera respuesta: solo con la información propia
    const sistema = INSTRUCCIONES_BASE + "\n" + textoNivel + "\n\n=== INFORMACIÓN DEL VEHÍCULO ===\n" + conocimiento.texto;
    const contents = construirContenido(historial, mensaje, imagen);

    const inicio = Date.now();
    let res = await preguntarGemini(sistema, contents);

    // 9. Búsqueda web solo si hace falta (o si se fuerza para pruebas)
    let fuentes: any[] = [];
    let consultaWeb = "";
    const usarWeb = buscar_web === true || (res.necesita_busqueda && buscar_web !== false);
    if (usarWeb) {
      consultaWeb = res.consulta_busqueda || `${mensaje} ${conocimiento.nombre}`.trim();
      fuentes = await buscarWeb(consultaWeb);
      if (fuentes.length > 0) {
        const bloqueWeb = fuentes.map((f, i) => `[${i + 1}] ${f.titulo} (${f.url})\n${f.contenido}`).join("\n\n");
        res = await preguntarGemini(sistema + "\n\n=== RESULTADOS WEB (apoyo) ===\n" + bloqueWeb + "\n" + INSTRUCCIONES_WEB, contents);
      }
    }
    const tiempo_ms = Date.now() - inicio;
    const fuentesCortas = fuentes.map((f) => ({ titulo: f.titulo, url: f.url }));

    // 10. Guardar la respuesta (con la llave de servicio)
    const { data: msg, error: errorIA } = await admin.from("mensaje").insert({
      conversacion_id, emisor: "asistente", contenido: res.respuesta,
      metadatos: {
        modelo: res.modelo, tiempo_ms, nivel, con_imagen: !!imagen,
        uso_web: fuentes.length > 0, consulta_web: consultaWeb, fuentes: fuentesCortas,
        derivar_taller: res.derivar_taller,
      },
    }).select("id").single();
    if (errorIA) console.error("No se guardó la respuesta:", errorIA);

    // 11. Guardar la ficha para el modo sin conexión
    let fichaGuardada = false;
    if (res.resumen_offline && res.palabras_clave.length >= 2) {
      const { error: errorFicha } = await admin.from("conocimiento_offline").insert({
        usuario_id: user.id,
        titulo: mensaje.slice(0, 80),
        contenido: res.resumen_offline,
        palabras_clave: res.palabras_clave,
        urgencia: res.derivar_taller ? "alta" : "media",
        origen: "consulta",
      });
      if (errorFicha) console.error("No se guardó la ficha offline:", errorFicha);
      else fichaGuardada = true;
    }

    return json({
      conversacion_id,
      mensaje_id: msg?.id ?? null,
      respuesta: res.respuesta,
      derivar_taller: res.derivar_taller,
      uso_web: fuentes.length > 0,
      fuentes: fuentesCortas,
      tiempo_ms,
      ficha_offline: fichaGuardada,
      guardado: !errorUsuario && !errorIA,
      error_guardado: errorUsuario?.message ?? errorIA?.message ?? null,
    });

  } catch (e) {
    console.error(e);
    if (e instanceof Error && e.message === "CUOTA") {
      return json({ error: "El asistente alcanzó su límite de uso. Intenta en unos minutos." }, 503);
    }
    return json({ error: "Error interno del servidor", detalle: String((e as any)?.message ?? e) }, 500);
  }
});