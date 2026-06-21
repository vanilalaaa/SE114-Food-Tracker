package com.SE114.food_tracker.data.local.entities
import com.SE114.food_tracker.core.sync.SyncStatus

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "category",
    indices = [
        Index("is_system"),
        Index("is_hidden"),
        Index("owner_id"),
        Index("sync_status")
    ]
)
data class Category(
    @PrimaryKey
    @ColumnInfo(name = "category_id")
    var categoryId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "owner_id")
    var ownerId: String? = null, // null đối với danh mục hệ thống (isSystem = true)

    var name: String,

    @ColumnInfo(name = "icon_url")
    var iconUrl: String,

    @ColumnInfo(name = "is_hidden")
    var isHidden: Boolean = false,

    @ColumnInfo(name = "is_system")
    var isSystem: Boolean = false,

    @ColumnInfo(name = "is_deleted")
    var isDeleted: Boolean = false,

    @ColumnInfo(name = "sync_status")
    var syncStatus: String = SyncStatus.PENDING.name,

    @ColumnInfo(name = "created_at")
    var createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    var updatedAt: Long = System.currentTimeMillis()
)