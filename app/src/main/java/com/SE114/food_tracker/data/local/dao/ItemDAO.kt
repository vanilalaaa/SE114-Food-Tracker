package com.SE114.food_tracker.data.local.dao

import androidx.room.*
import com.SE114.food_tracker.data.local.entities.Item
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDAO {
    // CRUD
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item)

    @Update
    suspend fun updateItem(item: Item)

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("SELECT * FROM item WHERE item_id = :id")
    fun getItemById(id: String): Flow<Item?> // Đã sửa từ Long thành String khớp với UUID

    @Query("SELECT * FROM item ORDER BY created_at DESC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM item WHERE category_id = :catId")
    fun getItemsByCategory(catId: String): Flow<List<Item>>

    // Lọc dữ liệu theo Ngày/Buổi cho màn hình Nhật ký
    @Query("SELECT * FROM item WHERE entry_date >= :startDate AND entry_date < :endDate ORDER BY time_type ASC, created_at DESC")
    fun getItemsByDay(startDate: Long, endDate: Long): Flow<List<Item>>

    @Query("SELECT * FROM item WHERE entry_date >= :startDate AND entry_date < :endDate ORDER BY entry_date DESC")
    fun getItemsByDateRange(startDate: Long, endDate: Long): Flow<List<Item>>

    @Query("SELECT * FROM item WHERE entry_date >= :startDate AND entry_date < :endDate AND category_id = :categoryId ORDER BY entry_date DESC")
    fun getItemsByCategoryAndDay(categoryId: String, startDate: Long, endDate: Long): Flow<List<Item>>

    // Phục vụ cơ chế Đồng bộ Local-First
    @Query("SELECT * FROM item WHERE sync_status = 'PENDING' OR sync_status = 'FAILED'")
    suspend fun getPendingItems(): List<Item>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItemsFromServer(items: List<Item>)

    @Query("UPDATE item SET sync_status = 'SYNCED' WHERE item_id = :itemId")
    suspend fun markSynced(itemId: String)

    @Query("UPDATE item SET sync_status = 'FAILED' WHERE item_id = :itemId")
    suspend fun markFailed(itemId: String)

    // Phục vụ Thống kê Chi tiêu Cá nhân
    @Query("SELECT COUNT(*) FROM item WHERE entry_date >= :startDate AND entry_date < :endDate AND wallet_id IS NULL")
    fun getItemCountForDay(startDate: Long, endDate: Long): Flow<Int>

    @Query("SELECT SUM(price) FROM item WHERE entry_date >= :startDate AND entry_date < :endDate AND wallet_id IS NULL")
    fun getTotalExpenseForDay(startDate: Long, endDate: Long): Flow<Double?> // Thêm Double? đề phòng ngày đó tổng = null

    // Lấy chi tiêu gom nhóm theo danh mục phục vụ vẽ biểu đồ Donut của Vico Chart
    @Query("SELECT category_id, SUM(price) as total FROM item WHERE entry_date >= :startDate AND entry_date < :endDate AND wallet_id IS NULL GROUP BY category_id ORDER BY total DESC")
    fun getPersonalExpenseByCategory(startDate: Long, endDate: Long): Flow<List<CategoryExpense>>
}
