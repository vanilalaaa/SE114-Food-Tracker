package com.SE114.food_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemDTO(
    @SerialName("item_id")       val id: String,
    @SerialName("owner_id")      val ownerId: String,
    @SerialName("category_id")   val categoryId: String,
    @SerialName("name")          val name: String,
    @SerialName("price")         val price: Double,
    @SerialName("currency_code") val currencyCode: String,
    @SerialName("time_type")     val timeType: Int,
    @SerialName("entry_date")    val entryDate: String,   // ISO 8601 date, e.g. "2026-06-07"
    @SerialName("rating")        val rating: Int? = null,
    @SerialName("note")          val note: String? = null,
    @SerialName("image_url")     val imageUrl: String? = null,
    @SerialName("is_shared")     val isShared: Boolean = false,
    @SerialName("wallet_id")     val walletId: String? = null,
    @SerialName("created_at")    val createdAt: String,   // ISO 8601 timestamp
    @SerialName("updated_at")    val updatedAt: String,   // ISO 8601 timestamp
    @SerialName("is_deleted")    val isDeleted: Boolean = false
)