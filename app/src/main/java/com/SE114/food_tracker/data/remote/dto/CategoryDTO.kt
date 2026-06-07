package com.SE114.food_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDTO(
    @SerialName("id")         val id: String,
    @SerialName("owner_id")   val ownerId: String? = null,
    @SerialName("name")       val name: String,
    @SerialName("icon_url")   val iconUrl: String,
    @SerialName("is_hidden")  val isHidden: Boolean = false,
    @SerialName("is_system")  val isSystem: Boolean = false,
    @SerialName("created_at") val createdAt: String
)