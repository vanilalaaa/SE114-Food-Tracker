package com.SE114.food_tracker.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDTO(
    val id: String,
    val ownerId: String? = null,
    val name: String,
    val iconUrl: String,
    val isHidden: Boolean = false,
    val isSystem: Boolean = false,
    val createdAt: String
)