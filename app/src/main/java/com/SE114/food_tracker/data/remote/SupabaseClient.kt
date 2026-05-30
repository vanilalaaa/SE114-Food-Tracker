package com.SE114.food_tracker.data.remote

import com.SE114.food_tracker.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth) {
            // Tự động làm mới session khi hết hạn
            alwaysAutoRefresh = true
        }

        install(Postgrest)
    }
}