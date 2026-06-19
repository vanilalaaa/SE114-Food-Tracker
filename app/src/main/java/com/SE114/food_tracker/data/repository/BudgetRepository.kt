package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.core.sync.SyncStatus
import com.SE114.food_tracker.data.local.dao.BudgetDAO
import com.SE114.food_tracker.data.local.entities.Budget
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for personal budget limits.
 *
 * Offline-first contract:
 * - Reads come from Room only (getBudget returns a Flow).
 * - Writes go to Room immediately with syncStatus = PENDING; Sync.kt pushes to Supabase.
 * - [setBudget] always reads the existing row first so unchanged fields are never null-ed out.
 *   (Budget table has one row per user with all 4 limits together.)
 *
 * RLS note: `budget_owner_all` policy enforces `user_id = auth.uid()`. Sync.kt must verify
 * `budget.userId == ownerId` before upserting or the row is silently dropped.
 */
@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDAO: BudgetDAO,
    private val supabaseClient: SupabaseClient
) {
    fun getCurrentUserId(): String? = supabaseClient.auth.currentUserOrNull()?.id

    /** Live stream of the user's budget row from Room. Returns null if not set yet. */
    fun getBudget(userId: String): Flow<Budget?> = budgetDAO.getBudgetByUserId(userId)

    /**
     * Save budget limits for [userId].
     *
     * Pass null for any field the user did not touch — this function merges with the
     * existing row so existing limits are preserved. If no row exists yet, all-null
     * fields are fine (the user simply hasn't set that granularity).
     */
    suspend fun setBudget(
        userId: String,
        daily: Double?,
        weekly: Double?,
        monthly: Double?,
        yearly: Double?
    ) {
        // Read current values so we never clobber fields the user didn't edit.
        // firstOrNull() collects one snapshot; returns null when no row exists yet
        // (user hasn't set a budget) — that is the correct behaviour here.
        val existing = budgetDAO.getBudgetByUserId(userId).firstOrNull()

        budgetDAO.upsertBudget(
            Budget(
                userId     = userId,
                daily      = daily   ?: existing?.daily,
                weekly     = weekly  ?: existing?.weekly,
                monthly    = monthly ?: existing?.monthly,
                yearly     = yearly  ?: existing?.yearly,
                syncStatus = SyncStatus.PENDING.name,
                updatedAt  = System.currentTimeMillis(),
                isDeleted  = false
            )
        )
    }

    // ── Sync helpers (consumed by Sync.kt worker) ──────────────────────────

    suspend fun getPendingBudgets(): List<Budget> = budgetDAO.getPendingBudgets()

    /**
     * Called by Sync.kt after a successful server pull.
     * Bypasses the merge logic in [setBudget] — the server row already has all 4 fields
     * populated and wins unconditionally (last-write-wins via updatedAt on server).
     * syncStatus is set to SYNCED so the row is not immediately re-pushed.
     */
    suspend fun upsertFromServer(budget: Budget) = budgetDAO.upsertBudget(
        budget.copy(syncStatus = SyncStatus.SYNCED.name)
    )

    suspend fun markSynced(userId: String) = budgetDAO.markSynced(userId)

    suspend fun markFailed(userId: String) = budgetDAO.markFailed(userId)
}