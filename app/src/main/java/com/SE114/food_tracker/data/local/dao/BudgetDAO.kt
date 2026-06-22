package com.SE114.food_tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.SE114.food_tracker.data.local.entities.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: Budget)

    @Query("SELECT * FROM budget WHERE user_id = :userId AND is_deleted = 0")
    fun getBudgetByUserId(userId: String): Flow<Budget?>

    @Query("UPDATE budget SET is_deleted = 1, sync_status = 'PENDING', updated_at = :updatedAt WHERE user_id = :userId")
    suspend fun softDeleteBudget(userId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM budget WHERE sync_status = 'PENDING' OR sync_status = 'FAILED'")
    suspend fun getPendingBudgets(): List<Budget>

    @Query("UPDATE budget SET sync_status = 'SYNCED' WHERE user_id = :userId")
    suspend fun markSynced(userId: String)

    @Query("UPDATE budget SET sync_status = 'FAILED' WHERE user_id = :userId")
    suspend fun markFailed(userId: String)

    /** Clears the local budget on explicit logout; re-pulls from server on next login. */
    @Query("DELETE FROM budget")
    suspend fun clearAll()
}