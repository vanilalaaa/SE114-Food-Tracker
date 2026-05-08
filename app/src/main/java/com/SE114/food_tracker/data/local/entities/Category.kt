package com.SE114.food_tracker.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category")
data class Category(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "category_id")
    var categoryId: Int = 0,

    var name: String,

    @ColumnInfo(name = "icon_url")
    var iconUrl: String,

    @ColumnInfo(name = "is_hidden")
    var isHidden: Boolean = false,

    @ColumnInfo(name = "is_system")
    var isSystem: Boolean = false
)