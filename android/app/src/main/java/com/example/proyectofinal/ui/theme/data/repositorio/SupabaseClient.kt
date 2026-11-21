package com.example.proyectofinal.data.repositorio

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest

val supabase = createSupabaseClient(
    supabaseUrl = "https://utqtxuxdizxavagckblf.supabase.co",
    supabaseKey = "sb_publishable_v0NLU7ZpsVESMxyb73m94g_VwXbHfwD"
) {
    install(Auth)
    install(Postgrest)
    install(Functions)
}
