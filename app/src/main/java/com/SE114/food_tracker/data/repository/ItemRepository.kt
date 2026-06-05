package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.dao.CategoryExpense
import com.SE114.food_tracker.data.local.dao.ItemDAO
import com.SE114.food_tracker.data.local.entities.Item
import com.SE114.food_tracker.feature.diary.DiaryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ItemRepository @Inject constructor(
    private val itemDAO: ItemDAO,
    private val categoryDAO: CategoryDAO
) {

    fun getAllItems(): Flow<List<Item>> = itemDAO.getAllItems()

    fun getItemsByDay(start: Long, end: Long): Flow<List<Item>> =
        itemDAO.getItemsByDay(start, end)

    fun getDiaryItemsByDay(start: Long, end: Long): Flow<List<DiaryItem>> =
        combine(
            itemDAO.getItemsByDay(start, end),
            categoryDAO.getAllCategories()
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
                    walletId = item.walletId,
                    entryDate = item.entryDate,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt
                )
            }
        }

    fun getItemsByDateRange(start: Long, end: Long): Flow<List<Item>> =
        itemDAO.getItemsByDateRange(start, end)

    fun getItemById(id: String): Flow<Item?> = itemDAO.getItemById(id)

    suspend fun insert(item: Item) = itemDAO.insertItem(item)

    suspend fun update(item: Item) = itemDAO.updateItem(item)

    suspend fun delete(item: Item) = itemDAO.deleteItem(item)

    suspend fun softDeleteDiaryItem(itemId: String) = itemDAO.softDeleteItem(itemId)

    suspend fun getPendingItems(): List<Item> = itemDAO.getPendingItems()

    suspend fun upsertItemsFromServer(items: List<Item>) = itemDAO.upsertItemsFromServer(items)

    suspend fun markSynced(itemId: String) = itemDAO.markSynced(itemId)

    suspend fun markFailed(itemId: String) = itemDAO.markFailed(itemId)

    fun getTotalExpenseForDay(start: Long, end: Long): Flow<Double?> =
        itemDAO.getTotalExpenseForDay(start, end)

    fun getItemCountForDay(start: Long, end: Long): Flow<Int> =
        itemDAO.getItemCountForDay(start, end)

    fun getPersonalExpenseByCategory(start: Long, end: Long): Flow<List<CategoryExpense>> =
        itemDAO.getPersonalExpenseByCategory(start, end)
}

private fun Int.toDiaryTimeLabel(): String =
    when (this) {
        0 -> "Sáng"
        1 -> "Trưa/Chiều"
        2 -> "Tối"
        else -> "Khác"
    }
