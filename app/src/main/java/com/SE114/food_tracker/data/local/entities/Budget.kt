package com.SE114.food_tracker.data.local.entities
import com.SE114.food_tracker.core.sync.SyncStatus

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget")
data class Budget(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,             // ID của User lấy từ Supabase Auth dạng UUID String

    val daily: Double? = null,
    val weekly: Double? = null,
    val monthly: Double? = null,
    val yearly: Double? = null,

    @ColumnInfo(name = "sync_status")
    var syncStatus: String = SyncStatus.PENDING.name,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)