package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.core.network.SessionProvider
import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.dao.CategoryExpense
import com.SE114.food_tracker.data.local.dao.ItemDAO
import com.SE114.food_tracker.data.local.entities.Item
import com.SE114.food_tracker.feature.diary.DiaryItem
import com.SE114.food_tracker.core.sync.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ItemRepository @Inject constructor(
    private val itemDAO: ItemDAO,
    private val categoryDAO: CategoryDAO,
    private val sessionProvider: SessionProvider
) {

    private fun owner(): String = sessionProvider.currentUserId().orEmpty()

    fun getAllItems(): Flow<List<Item>> = itemDAO.getAllItems(owner())

    fun getCurrentUserId(): String? = sessionProvider.currentUserId()

    suspend fun getItemByIdOneShot(id: String): Item? = itemDAO.getItemByIdOneShot(id)

    fun getItemsByDay(start: Long, end: Long): Flow<List<Item>> =
        itemDAO.getItemsByDay(owner(), start, end)

    suspend fun getDistinctEntryDates(ownerId: String): List<Long> =
        itemDAO.getDistinctEntryDates(ownerId)

    fun getDiaryItemsByDay(start: Long, end: Long): Flow<List<DiaryItem>> =
        combine(
            itemDAO.getItemsByDay(owner(), start, end),
            categoryDAO.getAllCategories(owner())
        ) { items, categories ->
            val categoriesById = categories.associateBy { it.categoryId }
            items.map { item ->
                val category = categoriesById[item.categoryId]
                DiaryItem(
                    itemId = item.itemId,
                    categoryId = item.categoryId,
                    categoryName = category?.name ?: "Khác",
                    categoryIconUrl = category?.iconUrl.orEmpty(),
                    name = item.name,
                    timeType = item.timeType,
                    timeLabel = item.timeType.toDiaryTimeLabel(),
                    price = item.price,
                    currencyCode = item.currencyCode,
                    rating = item.rating,
                    note = item.note,
                    imageUrl = item.imageUrl,
                    isShared = item.isShared,
                    entryDate = item.entryDate,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt
                )
            }
        }

    fun getItemsByDateRange(start: Long, end: Long): Flow<List<Item>> =
        itemDAO.getItemsByDateRange(owner(), start, end)

    fun getItemById(id: String): Flow<Item?> = itemDAO.getItemById(id)

    suspend fun insert(item: Item) {
        val pendingItem = item.copy(
            ownerId = owner(),
            syncStatus = SyncStatus.PENDING.name,
            updatedAt = System.currentTimeMillis()
        )
        itemDAO.insertItem(pendingItem)
    }

    suspend fun update(item: Item) {
        val pendingItem = item.copy(
            ownerId = owner(),
            syncStatus = SyncStatus.PENDING.name,
            updatedAt  = System.currentTimeMillis()
        )
        itemDAO.updateItem(pendingItem)
    }

    suspend fun delete(item: Item) = itemDAO.deleteItem(item)

    suspend fun softDeleteDiaryItem(itemId: String) = itemDAO.softDeleteItem(itemId)

    suspend fun getPendingItems(): List<Item> = itemDAO.getPendingItems(owner())

    suspend fun upsertItemsFromServer(items: List<Item>) = itemDAO.upsertItemsFromServer(items)

    suspend fun markSynced(itemId: String) = itemDAO.markSynced(itemId)

    suspend fun markFailed(itemId: String) = itemDAO.markFailed(itemId)

    suspend fun updateItemImageUrl(itemId: String, imageUrl: String) =
        itemDAO.updateItemImageUrl(itemId, imageUrl)

    fun getTotalExpenseForDay(start: Long, end: Long): Flow<Double?> =
        itemDAO.getTotalExpenseForDay(owner(), start, end)

    fun getItemCountForDay(start: Long, end: Long): Flow<Int> =
        itemDAO.getItemCountForDay(owner(), start, end)

    fun getPersonalExpenseByCategory(start: Long, end: Long): Flow<List<CategoryExpense>> =
        itemDAO.getPersonalExpenseByCategory(owner(), start, end)

    fun observeDistinctEntryDates(): Flow<List<Long>> =
        itemDAO.observeDistinctEntryDates(owner())
}

private fun Int.toDiaryTimeLabel(): String =
    when (this) {
        0 -> "Sáng"
        1 -> "Trưa"
        2 -> "Chiều"
        3 -> "Tối"
        else -> "Khác"
    }
