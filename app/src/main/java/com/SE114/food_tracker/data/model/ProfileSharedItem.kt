package com.SE114.food_tracker.data.model

data class ProfileSharedItem(
    val itemId: String,
    val name: String,
    val categoryName: String,
    val categoryIcon: String,
    val price: Double,
    val createdAt: Long,
    val timeLabel: String,
    val imageUrl: String?,
    val entryDate: String
)
