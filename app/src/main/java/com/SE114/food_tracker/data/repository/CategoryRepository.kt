package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.entities.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val categoryDAO: CategoryDAO
) {

    fun getAllCategories(): Flow<List<Category>> = categoryDAO.getAllCategories()

    fun getVisibleCategories(): Flow<List<Category>> = categoryDAO.getVisibleCategories()

    suspend fun insertCategory(category: Category) = categoryDAO.insert(category)

    suspend fun updateCategory(category: Category) = categoryDAO.update(category)

    suspend fun deleteCategory(category: Category) = categoryDAO.delete(category)

    suspend fun insert(category: Category) = insertCategory(category)

    suspend fun update(category: Category) = updateCategory(category)

    suspend fun delete(category: Category) = deleteCategory(category)
}
