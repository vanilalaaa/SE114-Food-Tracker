package com.SE114.food_tracker.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.SE114.food_tracker.data.remote.SupabaseItemService
import com.SE114.food_tracker.data.remote.dto.BudgetDTO
import com.SE114.food_tracker.data.remote.dto.CategoryDTO
import com.SE114.food_tracker.data.remote.mapper.DataMapper
import com.SE114.food_tracker.data.repository.BudgetRepository
import com.SE114.food_tracker.data.repository.CategoryRepository
import com.SE114.food_tracker.data.repository.FeedRepository
import com.SE114.food_tracker.data.repository.ItemRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

@HiltWorker
class Sync @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val supabaseItemService: SupabaseItemService,
    private val supabaseClient: SupabaseClient,
    private val budgetRepository: BudgetRepository,
    private val feedRepository: FeedRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.d("[Sync] ── worker started ──")

        if (!isDeviceOnline()) {
            Timber.w("[Sync] device offline → retry")
            return Result.retry()
        }

        val ownerId = supabaseClient.auth.currentUserOrNull()?.id
        if (ownerId == null) {
            Timber.w("[Sync] no auth session yet → retry")
            return Result.retry()
        }
        Timber.d("[Sync] authenticated as $ownerId")

        runCatching {
            val profileCount = supabaseClient.postgrest.from("profile")
                .select { filter { eq("id", ownerId) } }
                .decodeList<Map<String, String>>()
                .size
            Timber.d("[Sync] profile row count for this user = $profileCount")
            if (profileCount == 0) {
                Timber.e(
                    "[Sync] NO PROFILE ROW FOUND for uid=$ownerId. " +
                            "The `item` table has owner_id → profile(id) FK. " +
                            "Every item insert will fail until a profile row exists. " +
                            "Insert one manually in the Supabase Table Editor or via a trigger."
                )
            }
        }.onFailure { Timber.e(it, "[Sync] could not check profile row") }

        var anyError = false

        // ── STEP 1.1: push pending categories ────────────────────────────────
        try {
            val pending = categoryRepository.getPendingCategories()
            Timber.d("[Sync] categories pending = ${pending.size}")

            for (category in pending) {
                val isValidUuid = runCatching {
                    java.util.UUID.fromString(category.categoryId)
                    true
                }.getOrDefault(false)

                if (!isValidUuid) {
                    Timber.e(
                        "[Sync] SKIPPING category '${category.name}' — " +
                                "categoryId '${category.categoryId}' is not a valid UUID. " +
                                "Clear app data and relaunch to re-seed with proper UUIDs."
                    )
                    categoryRepository.markFailed(category.categoryId)
                    anyError = true
                    continue
                }

                // Soft-deleted custom category: push is_deleted=true to server, keep row locally
                if (category.isDeleted) {
                    val dto = with(DataMapper) { category.toDto() }
                    Timber.d("[Sync] soft-deleting category '${category.name}' id=${category.categoryId} on server")
                    runCatching {
                        supabaseClient.postgrest.from("category").upsert(dto)
                    }.onSuccess {
                        categoryRepository.markSynced(category.categoryId)
                        Timber.d("[Sync] ✓ category soft-deleted on server: ${category.name}")
                    }.onFailure { err ->
                        Timber.e(err, "[Sync] ✗ category soft-delete FAILED: ${category.name} — ${err.message}")
                        categoryRepository.markFailed(category.categoryId)
                        anyError = true
                    }
                    continue
                }

                val dto = with(DataMapper) { category.toDto() }
                Timber.d("[Sync] upserting category '${category.name}' id=${category.categoryId}")

                runCatching {
                    supabaseClient.postgrest.from("category").upsert(dto)
                }.onSuccess {
                    categoryRepository.markSynced(category.categoryId)
                    Timber.d("[Sync] ✓ category synced: ${category.name}")
                }.onFailure { err ->
                    Timber.e(err, "[Sync] ✗ category FAILED: ${category.name} — ${err.message}")
                    categoryRepository.markFailed(category.categoryId)
                    anyError = true
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "[Sync] fatal error in category push block")
            anyError = true
        }

        // ── STEP 1.2: push pending items ─────────────────────────────────────
        try {
            val pending = itemRepository.getPendingItems()
            Timber.d("[Sync] items pending = ${pending.size}")

            for (item in pending) {
                // Log the full DTO so you can inspect exactly what JSON is sent.
                val dto = with(DataMapper) { item.toDto(ownerId) }
                Timber.d(
                    "[Sync] upserting item '${item.name}' " +
                            "item_id=${dto.id} category_id=${dto.categoryId} " +
                            "entry_date=${dto.entryDate} price=${dto.price}"
                )

                supabaseItemService.uploadItem(item, ownerId)
                    .onSuccess {
                        itemRepository.markSynced(item.itemId)
                        Timber.d("[Sync] ✓ item synced: ${item.name}")
                    }
                    .onFailure { err ->
                        Timber.e(err, "[Sync] ✗ item FAILED: ${item.name} — ${err.message}")
                        itemRepository.markFailed(item.itemId)
                        anyError = true
                    }
            }
        } catch (e: Exception) {
            Timber.e(e, "[Sync] fatal error in item push block")
            anyError = true
        }

        // ── STEP 1.3: push pending budget ────────────────────────────────────
        try {
            val pendingBudgets = budgetRepository.getPendingBudgets()
            Timber.d("[Sync] budgets pending = ${pendingBudgets.size}")

            for (budget in pendingBudgets) {
                // ⚠️  RLS policy budget_owner_all: `auth.uid() = user_id`.
                // A mismatch never throws — postgrest silently returns 0 rows.
                // Detect it here and markFailed so the row doesn't loop as PENDING forever.
                if (budget.userId != ownerId) {
                    Timber.e("[Sync] SKIPPING budget — userId ${budget.userId} != auth uid $ownerId")
                    budgetRepository.markFailed(budget.userId)
                    anyError = true
                    continue
                }

                val dto = with(DataMapper) { budget.toDto() }
                runCatching {
                    supabaseClient.postgrest.from("budget").upsert(dto)
                }.onSuccess {
                    budgetRepository.markSynced(budget.userId)
                    Timber.d("[Sync] ✓ budget synced for userId=${budget.userId}")
                }.onFailure { err ->
                    Timber.e(err, "[Sync] ✗ budget FAILED — ${err.message}")
                    budgetRepository.markFailed(budget.userId)
                    anyError = true
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "[Sync] fatal error in budget push block")
            anyError = true
        }

        // ── STEP 1.4: push pending feed posts / likes / comments ─────────────
        try {
            val success = feedRepository.pushPendingToSupabase(ownerId)
            if (!success) anyError = true
        } catch (e: Exception) {
            Timber.e(e, "[Sync] fatal error in feed push block")
            anyError = true
        }

        // ── STEP 2.1: pull categories from server ────────────────────────────
        try {
            val remote = supabaseClient.postgrest.from("category")
                .select {
                    filter {
                        or {
                            eq("owner_id", ownerId)
                            eq("is_system", true)
                        }
                    }
                }.decodeList<CategoryDTO>()

            Timber.d("[Sync] pulled ${remote.size} categories from server")
            if (remote.isNotEmpty()) {
                val entities = remote.map { with(DataMapper) { it.toEntity() } }
                categoryRepository.upsertCategoriesFromServer(entities)
            }
        } catch (e: Exception) {
            Timber.e(e, "[Sync] failed pulling categories from server")
            anyError = true
        }

        // ── STEP 2.2: pull items from server (delta since last local update) ──
        try {
            val allLocal = itemRepository.getAllItems().firstOrNull().orEmpty()
            val maxUpdatedAt = allLocal.maxOfOrNull { it.updatedAt } ?: 0L
            Timber.d("[Sync] pulling items updated after epoch=$maxUpdatedAt")

            supabaseItemService.fetchItemsSince(ownerId, maxUpdatedAt)
                .onSuccess { dtos ->
                    Timber.d("[Sync] pulled ${dtos.size} new/updated items from server")
                    if (dtos.isNotEmpty()) {
                        val entities = dtos.map { with(DataMapper) { it.toEntity() } }
                        itemRepository.upsertItemsFromServer(entities)
                    }
                }
                .onFailure { err ->
                    Timber.e(err, "[Sync] failed pulling items from server — ${err.message}")
                    anyError = true
                }
        } catch (e: Exception) {
            Timber.e(e, "[Sync] fatal error in item pull block")
            anyError = true
        }

        // ── STEP 2.3: pull budget from server ────────────────────────────────
        try {
            val remoteBudgets = supabaseClient.postgrest.from("budget")
                .select { filter { eq("user_id", ownerId) } }
                .decodeList<BudgetDTO>()

            Timber.d("[Sync] pulled ${remoteBudgets.size} budget row(s) from server")
            remoteBudgets.firstOrNull()?.let { dto ->
                val entity = with(DataMapper) { dto.toEntity() }
                // upsertFromServer bypasses the merge logic — server wins unconditionally
                budgetRepository.upsertFromServer(entity)
                Timber.d("[Sync] ✓ budget pulled from server for userId=${entity.userId}")
            }
        } catch (e: Exception) {
            Timber.e(e, "[Sync] fatal error in budget pull block")
            anyError = true
        }

        // ── STEP 2.4: pull visible feed posts / likes / comments ─────────────
        try {
            val success = feedRepository.pullVisibleFromSupabase(ownerId)
            if (!success) anyError = true
        } catch (e: Exception) {
            Timber.e(e, "[Sync] fatal error in feed pull block")
            anyError = true
        }

        val result = if (anyError) Result.retry() else Result.success()
        Timber.d("[Sync] ── worker finished, result=$result ──")
        return result
    }

    private fun isDeviceOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cap = cm.getNetworkCapabilities(cm.activeNetwork)
        return cap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
