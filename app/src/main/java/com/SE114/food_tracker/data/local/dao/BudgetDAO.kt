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

    @Query("SELECT * FROM budget WHERE user_id = :userId")
    fun getBudgetByUserId(userId: String): Flow<Budget?>
}