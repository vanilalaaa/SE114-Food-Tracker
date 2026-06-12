package com.SE114.food_tracker.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile_cache")
data class UserProfileCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)