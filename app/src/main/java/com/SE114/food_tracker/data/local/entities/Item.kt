package com.SE114.food_tracker.data.local.entities

import androidx.room.*

@Entity(
    tableName = "item",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["category_id"],
        childColumns = ["category_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("category_id")]
)
data class Item(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "item_id")
    var itemId: Int = 0,

    @ColumnInfo(name = "category_id")
    var categoryId: Int,

    var name: String,

    @ColumnInfo(name = "time_type")
    var timeType: Int,

    var price: Double,
    var rating: Int? = null,
    var note: String? = null,

    @ColumnInfo(name = "image_path")
    var imagePath: String? = null,

    @ColumnInfo(name = "is_synced")
    var isSynced: String? = null,

    @ColumnInfo(name = "entry_date")
    var entryDate: Long,

    @ColumnInfo(name = "created_at")
    var createdAt: Long = System.currentTimeMillis()
)