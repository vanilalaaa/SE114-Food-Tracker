package com.SE114.food_tracker.data.local.dao

import androidx.room.*
import com.SE114.food_tracker.data.local.entities.Item
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item)

    @Update
    suspend fun updateItem(item: Item)

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("SELECT * FROM item ORDER BY created_at DESC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM item WHERE category_id = :catId")
    fun getItemsByCategory(catId: Int): Flow<List<Item>>

    @Query("SELECT * FROM item WHERE item_id = :id")
    fun getItemById(id: Long): Flow<Item>

    @Query("SELECT * FROM item WHERE entry_date >= :startDate AND entry_date < :endDate ORDER BY entry_date DESC")
    fun getItemsByDay(startDate: Long, endDate: Long): Flow<List<Item>>

    @Query("SELECT * FROM item WHERE entry_date >= :startDate AND entry_date < :endDate ORDER BY entry_date DESC")
    fun getItemsByDateRange(startDate: Long, endDate: Long): Flow<List<Item>>

    @Query("SELECT * FROM item WHERE entry_date >= :startDate AND entry_date < :endDate AND category_id = :categoryId ORDER BY entry_date DESC")
    fun getItemsByCategoryAndDay(categoryId: Int, startDate: Long, endDate: Long): Flow<List<Item>>

    @Query("SELECT COUNT(*) FROM item WHERE entry_date >= :startDate AND entry_date < :endDate")
    fun getItemCountForDay(startDate: Long, endDate: Long): Flow<Int>

    @Query("SELECT SUM(price) FROM item WHERE entry_date >= :startDate AND entry_date < :endDate")
    fun getTotalExpenseForDay(startDate: Long, endDate: Long): Flow<Double>
}