package com.SE114.food_tracker.data.local.dao

import androidx.room.*
import com.SE114.food_tracker.data.local.entities.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDAO {
    @Upsert
    suspend fun insert(category: Category)

    @Update
    suspend fun update(category: Category)

    // Soft-delete: marks is_deleted = 1 and queues for sync. Only for custom (is_system = false).
    @Query("UPDATE category SET is_deleted = 1, sync_status = 'PENDING', updated_at = :updatedAt WHERE category_id = :categoryId AND is_system = 0")
    suspend fun softDeleteCategory(categoryId: String, updatedAt: Long = System.currentTimeMillis())

    // Count active (non-deleted) items still linked to this category — used for RESTRICT check
    @Query("SELECT COUNT(*) FROM item WHERE category_id = :categoryId AND is_deleted = 0")
    suspend fun countActiveItemsForCategory(categoryId: String): Int

    // Exclude soft-deleted rows from all user-facing queries
    @Query("SELECT * FROM category WHERE is_deleted = 0 ORDER BY is_system DESC, name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM category WHERE is_hidden = 0 AND is_deleted = 0 ORDER BY is_system DESC, name ASC")
    fun getVisibleCategories(): Flow<List<Category>>

    // ── SYNC ──

    // Pending = needs upsert or delete pushed to server
    @Query("SELECT * FROM category WHERE sync_status = 'PENDING' OR sync_status = 'FAILED'")
    suspend fun getPendingCategories(): List<Category>

    @Upsert
    suspend fun upsertCategoriesFromServer(categories: List<Category>)

    @Query("UPDATE category SET sync_status = 'SYNCED' WHERE category_id = :categoryId")
    suspend fun markSynced(categoryId: String)

    @Query("UPDATE category SET sync_status = 'FAILED' WHERE category_id = :categoryId")
    suspend fun markFailed(categoryId: String)
}