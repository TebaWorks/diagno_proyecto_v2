# Diagno

Aplicación Android de **asistencia preventiva para la mantención automotriz**, con inteligencia artificial en línea y sin conexión.
Proyecto de Título TIHI84 – INACAP · Académico guía: Manuel Rojas
Equipo: Mathias Muñoz · Rody Sebastián Ñehuen · Mauricio Robles

## Funcionalidades
- Cuentas: registro en dos pasos con validaciones, verificación del correo con código, inicio con Google, recuperación de contraseña y eliminación de cuenta.
- Sesión persistente: una vez iniciada, la app funciona sin internet.
- Protección de datos (Ley 21.719): consentimiento explícito y versionado, política de privacidad en la app, opción de compartir revocable y supresión de la cuenta.
- Asistente en línea (Gemini + Tavily): responde con los datos del vehículo, calcula el estado de mantención, busca en la web solo si falta información y deriva a taller ante riesgos.
- Modo sin conexión: fichas sincronizadas al teléfono, búsqueda por palabras clave y, si está instalado, modelo Gemma local anclado a esas fichas.

## Tecnologías
| Capa | Tecnología |
|---|---|
| App | Kotlin + XML + Material 3, supabase-kt, Credential Manager, LiteRT-LM |
| Backend | Supabase: Auth, PostgreSQL con RLS, Storage, Edge Functions (TypeScript/Deno) |
| IA | Google Gemini (en línea), Gemma (en el dispositivo), Tavily (búsqueda web) |

## Estructura
- `android/` – aplicación (abrir esta carpeta en Android Studio)
- `supabase/functions/` – funciones `chat` y `eliminar-cuenta`
- `supabase/migrations/` – esquema y scripts SQL en orden
- `docs/` – informes, evidencia y documentación
- `subir.ps1` – despliega las funciones y sube los cambios

## Seguridad
El repositorio solo contiene la clave pública de Supabase, protegida por seguridad a nivel de fila. Las claves de los servicios externos se guardan como secretos del servidor.