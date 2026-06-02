package com.SE114.food_tracker.data.local.dao

import androidx.room.ColumnInfo

// Data class bổ trợ nhận kết quả thống kê chi tiêu theo danh mục
data class CategoryExpense(
    @ColumnInfo(name = "category_id") val categoryId: Int,
    val total: Double
)
