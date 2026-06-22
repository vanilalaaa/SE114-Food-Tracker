package com.SE114.food_tracker.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of the authenticated user id for the data layer. Repositories read
 * the owner id from here instead of reaching into supabase-kt directly, so every
 * Room query can be scoped to the current user. Returns null when no session exists.
 */
@Singleton
class SessionProvider @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    fun currentUserId(): String? = supabaseClient.auth.currentUserOrNull()?.id
}
