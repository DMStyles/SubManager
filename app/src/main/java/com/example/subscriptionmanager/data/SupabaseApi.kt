package com.example.subscriptionmanager.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseApi {
    const val SUPABASE_URL = "https://qcslcvzcxmttklgphnjf.supabase.co"
    const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFjc2xjdnpjeG10dGtsZ3BobmpmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODE5NzA0MDQsImV4cCI6MjA5NzU0NjQwNH0.1dDYJu01OR2Fw0pFGaW4t0Xw6Y8zqZhz5CpwWoPvlBU"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Auth) {
            scheme = "qcslcvzcxmttklgphnjf"
            host = "login-callback"
        }
    }
}
