package com.SE114.food_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemDTO(
    @SerialName("id") val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("category_id") val categoryId: Int,
    @SerialName("name") val name: String,
    @SerialName("price") val price: Double,
    @SerialName("currency_code") val currencyCode: String,
    @SerialName("time_type") val timeType: Int,
    @SerialName("entry_date") val entryDate: String, // Chuỗi ngày ISO 8601 (VD: "2026-06-03")
    @SerialName("rating") val rating: Int? = null,
    @SerialName("note") val note: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("is_shared") val isShared: Boolean = false,
    @SerialName("wallet_id") val walletId: String? = null, // UUID nhóm quỹ nếu có
    @SerialName("created_at") val createdAt: String, // Chuỗi ISO timestamp
    @SerialName("updated_at") val updatedAt: String  // Chuỗi ISO timestamp
)