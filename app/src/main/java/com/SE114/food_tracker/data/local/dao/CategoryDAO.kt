package com.SE114.food_tracker.data.local.dao

import androidx.room.*
import com.SE114.food_tracker.data.local.entities.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    // Lấy tất cả danh mục (bao gồm cả danh mục đã ẩn - dùng cho trang quản lý)
    @Query("SELECT * FROM category ORDER BY is_system DESC, name ASC")
    fun getAllCategories(): Flow<List<Category>>

    // CHỈ lấy các danh mục đang hiển thị (Dùng hiển thị lên List cho user chọn khi add món ăn)
    @Query("SELECT * FROM category WHERE is_hidden = 0 ORDER BY is_system DESC, name ASC")
    fun getVisibleCategories(): Flow<List<Category>>

    // ── BỔ SUNG CÁC HÀM PHỤC VỤ ĐỒNG BỘ (SYNC LOGIC) ──

    @Query("SELECT * FROM category WHERE sync_status = 'PENDING' OR sync_status = 'FAILED'")
    suspend fun getPendingCategories(): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategoriesFromServer(categories: List<Category>)

    @Query("UPDATE category SET sync_status = 'SYNCED' WHERE category_id = :categoryId")
    suspend fun markSynced(categoryId: String)

    @Query("UPDATE category SET sync_status = 'FAILED' WHERE category_id = :categoryId")
    suspend fun markFailed(categoryId: String)
}