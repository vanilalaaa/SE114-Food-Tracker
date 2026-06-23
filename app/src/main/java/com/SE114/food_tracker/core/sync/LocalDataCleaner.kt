package com.SE114.food_tracker.core.sync

import com.SE114.food_tracker.data.local.dao.BudgetDAO
import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.dao.ItemDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wipes the device's user-owned local tables on explicit logout, so a different
 * account never sees the previous user's rows. Defense-in-depth: all reads are
 * also owner-scoped, but clearing keeps the local DB honest on account switch.
 *
 * Tables cleared: `item` (all), `category` (custom only — system rows with
 * owner_id NULL are kept), `budget` (all). The correct per-user data re-pulls
 * from Supabase on the next login via [SyncManager.startInitialSync].
 */
@Singleton
class LocalDataCleaner @Inject constructor(
    private val itemDAO: ItemDAO,
    private val categoryDAO: CategoryDAO,
    private val budgetDAO: BudgetDAO
) {
    suspend fun clearUserOwnedData() = withContext(Dispatchers.IO) {
        itemDAO.clearAll()
        categoryDAO.clearUserCategories()
        budgetDAO.clearAll()
    }
}
