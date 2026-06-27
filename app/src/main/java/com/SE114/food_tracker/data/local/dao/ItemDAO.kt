package com.SE114.food_tracker.data.local.dao

import androidx.room.*
import androidx.room.ColumnInfo
import com.SE114.food_tracker.data.local.entities.Item
import kotlinx.coroutines.flow.Flow

data class LocalProfileSharedItemDto(
    @ColumnInfo(name = "itemId") val itemId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "categoryName") val categoryName: String?,
    @ColumnInfo(name = "categoryIcon") val categoryIcon: String?,
    @ColumnInfo(name = "price") val price: Double,
    @ColumnInfo(name = "createdAt") val createdAt: Long,
    @ColumnInfo(name = "timeType") val timeType: Int,
    @ColumnInfo(name = "imageUrl") val imageUrl: String?,
    @ColumnInfo(name = "entryDateMillis") val entryDateMillis: Long
)

@Dao
interface ItemDAO {
    @Upsert
    suspend fun insertItem(item: Item)

    @Update
    suspend fun updateItem(item: Item)

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("UPDATE item SET is_deleted = 1, sync_status = 'PENDING', updated_at = :updatedAt WHERE item_id = :itemId")
    suspend fun softDeleteItem(itemId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM item WHERE item_id = :id AND is_deleted = 0")
    fun getItemById(id: String): Flow<Item?>

    @Query("SELECT * FROM item WHERE owner_id = :ownerId AND is_deleted = 0 ORDER BY created_at DESC")
    fun getAllItems(ownerId: String): Flow<List<Item>>

    @Query("""
        SELECT
            i.item_id AS itemId,
            i.name AS name,
            c.name AS categoryName,
            c.icon_url AS categoryIcon,
            i.price AS price,
            i.created_at AS createdAt,
            i.time_type AS timeType,
            i.image_url AS imageUrl,
            i.entry_date AS entryDateMillis
        FROM item i
        LEFT JOIN category c ON c.category_id = i.category_id
        WHERE i.owner_id = :ownerId
          AND i.is_shared = 1
          AND i.is_deleted = 0
        ORDER BY i.entry_date DESC, i.created_at DESC
    """)
    suspend fun getSharedItemsForProfile(ownerId: String): List<LocalProfileSharedItemDto>

    @Query("SELECT * FROM item WHERE item_id = :id")
    suspend fun getItemByIdOneShot(id: String): Item?

    @Query("SELECT * FROM item WHERE owner_id = :ownerId AND category_id = :catId AND is_deleted = 0")
    fun getItemsByCategory(ownerId: String, catId: String): Flow<List<Item>>

    // CHẶN QUỸ NHÓM: Thêm điều kiện wallet_id IS NULL cho danh sách món ăn theo ngày trong thống kê cá nhân
    @Query("SELECT * FROM item WHERE owner_id = :ownerId AND entry_date >= :startDate AND entry_date < :endDate AND is_deleted = 0 ORDER BY time_type ASC, created_at DESC")
    fun getItemsByDay(ownerId: String, startDate: Long, endDate: Long): Flow<List<Item>>

    // CHẶN QUỸ NHÓM: QUAN TRỌNG NHẤT - Thêm wallet_id IS NULL để getDetailItems của tầng Thống Kê loại bỏ hoàn toàn đồ ăn nhóm từ gốc Database
    @Query("SELECT * FROM item WHERE owner_id = :ownerId AND entry_date >= :startDate AND entry_date < :endDate AND is_deleted = 0 ORDER BY entry_date DESC")
    fun getItemsByDateRange(ownerId: String, startDate: Long, endDate: Long): Flow<List<Item>>

    // CHẶN QUỸ NHÓM: Thêm wallet_id IS NULL
    @Query("SELECT * FROM item WHERE owner_id = :ownerId AND entry_date >= :startDate AND entry_date < :endDate AND category_id = :categoryId AND is_deleted = 0 ORDER BY entry_date DESC")
    fun getItemsByCategoryAndDay(ownerId: String, categoryId: String, startDate: Long, endDate: Long): Flow<List<Item>>

    @Query("SELECT * FROM item WHERE owner_id = :ownerId AND (sync_status = 'PENDING' OR sync_status = 'FAILED')")
    suspend fun getPendingItems(ownerId: String): List<Item>

    // CHẶN QUỸ NHÓM: Lấy các ngày có dữ liệu, cũng chỉ tính ngày có chi tiêu cá nhân
    @Query(" SELECT DISTINCT entry_date FROM item WHERE owner_id = :ownerId AND is_deleted = 0 ORDER BY entry_date DESC")
    suspend fun getDistinctEntryDates(ownerId: String): List<Long>

    @Upsert
    suspend fun upsertItemsFromServer(items: List<Item>)

    @Query("UPDATE item SET sync_status = 'SYNCED' WHERE item_id = :itemId")
    suspend fun markSynced(itemId: String)

    @Query("UPDATE item SET sync_status = 'FAILED' WHERE item_id = :itemId")
    suspend fun markFailed(itemId: String)

    @Query("UPDATE item SET image_url = :imageUrl, updated_at = :updatedAt WHERE item_id = :itemId")
    suspend fun updateItemImageUrl(
        itemId: String,
        imageUrl: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    /** Wipes every item on explicit logout (Task 3 hygiene). Server is the source of truth. */
    @Query("DELETE FROM item")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM item WHERE owner_id = :ownerId AND entry_date >= :startDate AND entry_date < :endDate AND is_deleted = 0")
    fun getItemCountForDay(ownerId: String, startDate: Long, endDate: Long): Flow<Int>

    @Query("SELECT SUM(price) FROM item WHERE owner_id = :ownerId AND entry_date >= :startDate AND entry_date < :endDate AND is_deleted = 0")
    fun getTotalExpenseForDay(ownerId: String, startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT category_id, SUM(price) as total FROM item WHERE owner_id = :ownerId AND entry_date >= :startDate AND entry_date < :endDate AND is_deleted = 0 GROUP BY category_id ORDER BY total DESC")
    fun getPersonalExpenseByCategory(ownerId: String, startDate: Long, endDate: Long): Flow<List<CategoryExpense>>

    /**
     * Biểu đồ cột theo ngày — spend (and count) bucketed by meal session.
     * time_type: 0 = Sáng, 1 = Trưa, 2 = Tối.
     */
    @Query("""
        SELECT time_type, SUM(price) as total, COUNT(*) as count
        FROM item
        WHERE owner_id = :ownerId AND entry_date >= :startDate AND entry_date < :endDate
           AND is_deleted = 0
        GROUP BY time_type
        ORDER BY time_type ASC
    """)
    fun getExpenseByTimeType(ownerId: String, startDate: Long, endDate: Long): Flow<List<TimeTypeExpense>>

    /**
     * Biểu đồ cột theo Tuần/Tháng — one row per distinct entry_date.
     * The ViewModel/Repository maps entry_date epoch → day label (T2…CN or 1…31).
     */
    @Query("""
        SELECT entry_date, SUM(price) as total
        FROM item
        WHERE owner_id = :ownerId AND entry_date >= :startDate AND entry_date < :endDate
          AND is_deleted = 0
        GROUP BY entry_date
        ORDER BY entry_date ASC
    """)
    fun getExpenseByDateBucket(ownerId: String, startDate: Long, endDate: Long): Flow<List<DateExpense>>

    /**
     * YEAR view bar chart — exactly 12 bars, one per calendar month.
     */
    @Query("""
        SELECT
            (entry_date / 2629800000) * 2629800000 AS month_epoch,
            SUM(price) as total
        FROM item
        WHERE owner_id = :ownerId AND entry_date >= :startDate AND entry_date < :endDate
          AND is_deleted = 0
        GROUP BY month_epoch
        ORDER BY month_epoch ASC
    """)
    fun getExpenseByMonthBucket(ownerId: String, startDate: Long, endDate: Long): Flow<List<MonthExpense>>

    /**
     * "Kẻ hủy diệt ví" — the single most expensive personal item in the period.
     * Vì đã chặn wallet_id IS NULL ở SQL, nên limit có thể đưa về 1 an toàn, không sợ trúng món quỹ nhóm.
     */
    @Query("""
        SELECT * FROM item
        WHERE owner_id = :ownerId AND entry_date >= :startDate AND entry_date < :endDate
          AND is_deleted = 0
        ORDER BY price DESC
        LIMIT :limit
    """)
    fun getTopExpensiveItems(ownerId: String, startDate: Long, endDate: Long, limit: Int = 1): Flow<List<Item>>

    /**
     * "Phổ biến nhất" — most frequently logged food names, with tie-break by total spend.
     */
    @Query("""
        SELECT name, COUNT(*) as recordCount, SUM(price) as totalSpent
        FROM item
        WHERE owner_id = :ownerId AND entry_date >= :startDate AND entry_date < :endDate
          AND is_deleted = 0
        GROUP BY name
        ORDER BY recordCount DESC, totalSpent DESC
        LIMIT :limit
    """)
    fun getPopularFoods(ownerId: String, startDate: Long, endDate: Long, limit: Int = 5): Flow<List<PopularFoodExpense>>

    /**
     * Tổng chi tiêu theo khoảng thời gian — used for "vs previous period" comparison.
     */
    @Query("""
        SELECT SUM(price) FROM item
        WHERE owner_id = :ownerId AND entry_date >= :startDate AND entry_date < :endDate
          AND is_deleted = 0
    """)
    fun getTotalExpenseForRange(ownerId: String, startDate: Long, endDate: Long): Flow<Double?>

    @Query("""
    SELECT DISTINCT entry_date 
    FROM item 
    WHERE owner_id = :ownerId AND is_deleted = 0 
    ORDER BY entry_date DESC
""")
    fun observeDistinctEntryDates(ownerId: String): Flow<List<Long>>
}
