package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.entities.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepository @Inject constructor(private val categoryDAO: CategoryDAO) {

    // Lấy toàn bộ (Dùng cho trang cấu hình quản lý danh mục)
    fun getAllCategories(): Flow<List<Category>> = categoryDAO.getAllCategories()

    // CHỈ lấy danh mục đang hiển thị (Dùng cho List/Spinner để user chọn khi add món ăn)
    fun getVisibleCategories(): Flow<List<Category>> = categoryDAO.getVisibleCategories()

    suspend fun insert(category: Category) = categoryDAO.insert(category)

    suspend fun update(category: Category) = categoryDAO.update(category)

    suspend fun delete(category: Category) = categoryDAO.delete(category)
}