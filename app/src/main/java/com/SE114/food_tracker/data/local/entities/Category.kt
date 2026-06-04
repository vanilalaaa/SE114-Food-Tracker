package com.SE114.food_tracker.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "category",
    indices = [Index("is_system"), Index("is_hidden")]
)
data class Category(
    @PrimaryKey
    @ColumnInfo(name = "category_id")
    var categoryId: String = UUID.randomUUID().toString(), // UUID khớp schema Supabase, tránh đè id chéo thiết bị khi sync

    var name: String,

    @ColumnInfo(name = "icon_url")
    var iconUrl: String,

    @ColumnInfo(name = "is_hidden")
    var isHidden: Boolean = false,

    @ColumnInfo(name = "is_system")
    var isSystem: Boolean = false
)