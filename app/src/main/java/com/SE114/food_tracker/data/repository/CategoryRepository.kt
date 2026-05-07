package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.entities.Category
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDAO: CategoryDAO) {

    fun getAllCategories(): Flow<List<Category>> = categoryDAO.getAllCategories()

    suspend fun insert(category: Category) = categoryDAO.insert(category)

    suspend fun update(category: Category) = categoryDAO.update(category)

    suspend fun delete(category: Category) = categoryDAO.delete(category)
}