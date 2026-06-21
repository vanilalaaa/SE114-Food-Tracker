package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.entities.Category
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val categoryDAO: CategoryDAO,
    private val supabaseClient: SupabaseClient
) {

    fun getAllCategories(): Flow<List<Category>> = categoryDAO.getAllCategories()

    fun getVisibleCategories(): Flow<List<Category>> = categoryDAO.getVisibleCategories()

    fun getCurrentUserId(): String? = supabaseClient.auth.currentUserOrNull()?.id

    suspend fun insertCategory(category: Category) = categoryDAO.insert(category)

    suspend fun updateCategory(category: Category) = categoryDAO.update(category)

    suspend fun updateCustomCategoryDetails(categoryId: String, name: String, iconUrl: String) =
        categoryDAO.updateCustomCategoryDetails(categoryId, name, iconUrl)

    suspend fun countActiveItemsForCategory(categoryId: String): Int =
        categoryDAO.countActiveItemsForCategory(categoryId)

    suspend fun softDeleteCategory(categoryId: String) =
        categoryDAO.softDeleteCategory(categoryId)

    // ── SYNC ──

    suspend fun getPendingCategories(): List<Category> = categoryDAO.getPendingCategories()

    suspend fun upsertCategoriesFromServer(categories: List<Category>) =
        categoryDAO.upsertCategoriesFromServer(categories)

    suspend fun markSynced(categoryId: String) = categoryDAO.markSynced(categoryId)

    suspend fun markFailed(categoryId: String) = categoryDAO.markFailed(categoryId)
}
