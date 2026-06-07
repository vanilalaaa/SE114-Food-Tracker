package com.SE114.food_tracker.data.local.entities

import androidx.room.*
import java.util.UUID

@Entity(
    tableName = "item",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["category_id"],
        childColumns = ["category_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("category_id"),
        Index("sync_status"),
        Index("entry_date")
    ]
)
data class Item(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    var itemId: String = UUID.randomUUID().toString(), // Chuyển sang UUID String để sync không bị đè id chéo thiết bị

    @ColumnInfo(name = "category_id")
    var categoryId: String,

    var name: String,

    @ColumnInfo(name = "time_type")
    var timeType: Int,              // 0 = Sáng, 1 = Trưa/Chiều, 2 = Tối

    var price: Double,

    @ColumnInfo(name = "currency_code")
    var currencyCode: String = "VND", // Quản lý đa tiền tệ theo đặc tả

    var rating: Int? = null,
    var note: String? = null,

    @ColumnInfo(name = "image_url")
    var imageUrl: String? = null,   // Lưu path/url ảnh món ăn từ Supabase Storage

    @ColumnInfo(name = "is_shared")
    var isShared: Boolean = false,  // Chia sẻ nhật ký với bạn bè

    @ColumnInfo(name = "wallet_id")
    var walletId: String? = null,   // null = cá nhân, có giá trị = thuộc quỹ nhóm

    @ColumnInfo(name = "sync_status")
    var syncStatus: String = SyncStatus.PENDING.name,

    @ColumnInfo(name = "entry_date")
    var entryDate: Long,            // Epoch millis (UTC) của ngày ghi nhật ký

    @ColumnInfo(name = "updated_at")
    var updatedAt: Long = System.currentTimeMillis(), // Dùng so sánh Last-Write-Wins khi sync

    @ColumnInfo(name = "created_at")
    var createdAt: Long = System.currentTimeMillis()
)
