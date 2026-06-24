package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.core.network.SessionProvider
import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.entities.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val categoryDAO: CategoryDAO,
    private val sessionProvider: SessionProvider
) {

    private fun owner(): String = sessionProvider.currentUserId().orEmpty()

    fun getAllCategories(): Flow<List<Category>> = categoryDAO.getAllCategories(owner())

    fun getVisibleCategories(): Flow<List<Category>> = categoryDAO.getVisibleCategories(owner())

    fun getCurrentUserId(): String? = sessionProvider.currentUserId()

    suspend fun insertCategory(category: Category) = categoryDAO.insert(category)

    suspend fun updateCategory(category: Category) = categoryDAO.update(category)

    suspend fun updateCustomCategoryDetails(categoryId: String, name: String, iconUrl: String) =
        categoryDAO.updateCustomCategoryDetails(categoryId, name, iconUrl)

    suspend fun updateCategoryVisibility(categoryId: String, isHidden: Boolean) =
        categoryDAO.updateCategoryVisibility(categoryId, isHidden)

    suspend fun countActiveItemsForCategory(categoryId: String): Int =
        categoryDAO.countActiveItemsForCategory(categoryId)

    suspend fun softDeleteCategory(categoryId: String) =
        categoryDAO.softDeleteCategory(categoryId)
    suspend fun getCategoryByIdOneShot(id: String): Category? =
        categoryDAO.getCategoryByIdOneShot(id)

    // ── SYNC ──

    suspend fun getPendingCategories(): List<Category> = categoryDAO.getPendingCategories(owner())

    suspend fun upsertCategoriesFromServer(categories: List<Category>) =
        categoryDAO.upsertCategoriesFromServer(categories)

    suspend fun markSynced(categoryId: String) = categoryDAO.markSynced(categoryId)

    suspend fun markFailed(categoryId: String) = categoryDAO.markFailed(categoryId)
}
