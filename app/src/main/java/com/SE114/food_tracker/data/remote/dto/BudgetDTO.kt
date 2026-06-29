package com.SE114.food_tracker.data.remote.dto
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.EncodeDefault

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BudgetDTO(
    @SerialName("user_id") val userId: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("daily")   val daily: Double?   = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("weekly")  val weekly: Double?  = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("monthly") val monthly: Double? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("yearly")  val yearly: Double?  = null,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)