package com.SE114.food_tracker.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "feed_hidden_post",
    primaryKeys = ["post_id", "user_id"],
    indices = [Index("user_id")]
)
data class FeedHiddenPost(
    @ColumnInfo(name = "post_id")
    val postId: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "hidden_at")
    val hiddenAt: Long = System.currentTimeMillis()
)
