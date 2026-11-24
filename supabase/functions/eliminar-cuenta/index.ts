import { createClient } from "npm:@supabase/supabase-js@2";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

function json(cuerpo: unknown, status = 200) {
  return new Response(JSON.stringify(cuerpo), {
    status,
    headers: { ...cors, "Content-Type": "application/json; charset=utf-8" },
  });
}

// Elimina la cuenta del usuario que hace la solicitud (derecho de supresión, Ley 21.719)
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });

  try {
    const url = Deno.env.get("SUPABASE_URL")!;

    // 1. Identifica al usuario con su propia sesión: nadie puede borrar otra cuenta
    const sb = createClient(url, Deno.env.get("SUPABASE_ANON_KEY")!, {
      global: { headers: { Authorization: req.headers.get("Authorization") ?? "" } },
    });
    const { data: { user } } = await sb.auth.getUser();
    if (!user) return json({ error: "No autenticado" }, 401);

    // 2. Confirmación explícita desde la app
    const body = await req.json().catch(() => ({}));
    if (body?.confirmacion !== "ELIMINAR") return json({ error: "Falta la confirmación" }, 400);

    const admin = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);

    // 3. Fotos del usuario en el almacenamiento (carpeta con su id)
    const { data: archivos } = await admin.storage.from("fotos").list(user.id, { limit: 1000 });
    if (archivos && archivos.length > 0) {
      await admin.storage.from("fotos").remove(archivos.map((a) => `${user.id}/${a.name}`));
    }

    // 4. La cuenta: el borrado en cascada elimina perfil, vehículos, conversaciones,
    //    mensajes, mantenciones, consultas y fichas offline
    const { error } = await admin.auth.admin.deleteUser(user.id);
    if (error) throw error;

    console.log("Cuenta eliminada:", user.id);   // solo el id, sin datos personales
    return json({ ok: true });
  } catch (e) {
    console.error(e);
    return json({ error: "No se pudo eliminar la cuenta" }, 500);
  }
});