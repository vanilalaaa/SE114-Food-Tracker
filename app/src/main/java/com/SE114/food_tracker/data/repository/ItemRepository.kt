package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.data.local.dao.ItemDAO
import com.SE114.food_tracker.data.local.entities.Item
import kotlinx.coroutines.flow.Flow

class ItemRepository(private val itemDAO: ItemDAO) {

    fun getAllItems(): Flow<List<Item>> = itemDAO.getAllItems()

    fun getItemsByDay(start: Long, end: Long): Flow<List<Item>> =
        itemDAO.getItemsByDay(start, end)

    fun getTotalExpenseForDay(start: Long, end: Long): Flow<Double?> =
        itemDAO.getTotalExpenseForDay(start, end)

    fun getItemCountForDay(start: Long, end: Long): Flow<Int?> =
        itemDAO.getItemCountForDay(start, end)

    suspend fun insert(item: Item) = itemDAO.insertItem(item)

    suspend fun update(item: Item) = itemDAO.updateItem(item)

    suspend fun delete(item: Item) = itemDAO.deleteItem(item)
}