package com.example.proyectofinal.data.repositorio

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import kotlin.time.Duration.Companion.seconds

val supabase = createSupabaseClient(
    supabaseUrl = "https://utqtxuxdizxavagckblf.supabase.co",
    supabaseKey = "sb_publishable_v0NLU7ZpsVESMxyb73m94g_VwXbHfwD"
) {
    // La IA puede tardar más de 10 s cuando busca en internet (2 consultas a Gemini + Tavily)
    requestTimeout = 60.seconds

    install(Auth)
    install(Postgrest)
    install(Functions)
}