package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.data.local.dao.CategoryExpense
import com.SE114.food_tracker.data.local.dao.ItemDAO
import com.SE114.food_tracker.data.local.entities.Item
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ItemRepository @Inject constructor(private val itemDAO: ItemDAO) {

    // --- Các hàm cơ bản phục vụ UI Nhật ký ---
    fun getAllItems(): Flow<List<Item>> = itemDAO.getAllItems()

    fun getItemsByDay(start: Long, end: Long): Flow<List<Item>> =
        itemDAO.getItemsByDay(start, end)

    fun getItemById(id: String): Flow<Item?> = itemDAO.getItemById(id)

    suspend fun insert(item: Item) = itemDAO.insertItem(item)

    suspend fun update(item: Item) = itemDAO.updateItem(item)

    suspend fun delete(item: Item) = itemDAO.deleteItem(item)


    // --- Các hàm phục vụ cho tính năng Đồng bộ  ---
    suspend fun getPendingItems(): List<Item> = itemDAO.getPendingItems()

    suspend fun upsertItemsFromServer(items: List<Item>) = itemDAO.upsertItemsFromServer(items)

    suspend fun markSynced(itemId: String) = itemDAO.markSynced(itemId)

    suspend fun markFailed(itemId: String) = itemDAO.markFailed(itemId)


    // --- Các hàm phục vụ cho Thống kê cá nhân (Loại bỏ ví nhóm) ---
    fun getTotalExpenseForDay(start: Long, end: Long): Flow<Double?> =
        itemDAO.getTotalExpenseForDay(start, end)

    fun getItemCountForDay(start: Long, end: Long): Flow<Int> =
        itemDAO.getItemCountForDay(start, end)

    fun getPersonalExpenseByCategory(start: Long, end: Long): Flow<List<CategoryExpense>> =
        itemDAO.getPersonalExpenseByCategory(start, end)
}