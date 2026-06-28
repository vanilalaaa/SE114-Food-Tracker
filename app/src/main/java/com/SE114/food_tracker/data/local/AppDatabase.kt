package com.SE114.food_tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.SE114.food_tracker.data.local.dao.BudgetDAO
import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.dao.ItemDAO
import com.SE114.food_tracker.data.local.entities.Budget
import com.SE114.food_tracker.data.local.entities.Category
import com.SE114.food_tracker.data.local.entities.Item
import com.SE114.food_tracker.data.local.entities.Conversation
import com.SE114.food_tracker.data.local.entities.ConversationParticipant
import com.SE114.food_tracker.data.local.entities.Message
import com.SE114.food_tracker.data.local.entities.FriendshipEntity
import com.SE114.food_tracker.data.local.entities.UserProfileCacheEntity
import com.SE114.food_tracker.data.local.entities.FeedComment
import com.SE114.food_tracker.data.local.entities.FeedHiddenPost
import com.SE114.food_tracker.data.local.entities.FeedLike
import com.SE114.food_tracker.data.local.entities.FeedPost
import com.SE114.food_tracker.data.local.dao.ChatDAO
import com.SE114.food_tracker.data.local.dao.FeedDAO
import com.SE114.food_tracker.data.local.dao.FriendDAO

@Database(
    entities = [
        Category::class,
        Item::class,
        Budget::class,
        Conversation::class,
        ConversationParticipant::class,
        Message::class,
        FriendshipEntity::class,
        UserProfileCacheEntity::class,
        FeedPost::class,
        FeedLike::class,
        FeedComment::class,
        FeedHiddenPost::class
    ],
    version = 19,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDAO(): CategoryDAO
    abstract fun itemDAO(): ItemDAO
    abstract fun budgetDAO(): BudgetDAO
    abstract fun chatDao(): ChatDAO
    abstract fun friendDao(): FriendDAO
    abstract fun feedDao(): FeedDAO

    companion object {

    }
}

private fun SupportSQLiteDatabase.createFeedTables() {
    execSQL(
        """
        CREATE TABLE IF NOT EXISTS feed_post (
            post_id TEXT NOT NULL PRIMARY KEY,
            owner_id TEXT NOT NULL,
            owner_name TEXT NOT NULL,
            item_id TEXT,
            image_url TEXT NOT NULL,
            caption TEXT NOT NULL,
            visibility TEXT NOT NULL DEFAULT 'friends',
            sync_status TEXT NOT NULL DEFAULT 'PENDING',
            is_deleted INTEGER NOT NULL DEFAULT 0,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
        """.trimIndent()
    )
    execSQL("CREATE INDEX IF NOT EXISTS index_feed_post_owner_id ON feed_post(owner_id)")
    execSQL("CREATE INDEX IF NOT EXISTS index_feed_post_item_id ON feed_post(item_id)")
    execSQL("CREATE INDEX IF NOT EXISTS index_feed_post_created_at ON feed_post(created_at)")
    execSQL("CREATE INDEX IF NOT EXISTS index_feed_post_sync_status ON feed_post(sync_status)")

    execSQL(
        """
        CREATE TABLE IF NOT EXISTS feed_like (
            like_id TEXT NOT NULL PRIMARY KEY,
            post_id TEXT NOT NULL,
            user_id TEXT NOT NULL,
            sync_status TEXT NOT NULL DEFAULT 'PENDING',
            is_deleted INTEGER NOT NULL DEFAULT 0,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
        """.trimIndent()
    )
    execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_feed_like_post_id_user_id ON feed_like(post_id, user_id)")
    execSQL("CREATE INDEX IF NOT EXISTS index_feed_like_sync_status ON feed_like(sync_status)")

    execSQL(
        """
        CREATE TABLE IF NOT EXISTS feed_comment (
            comment_id TEXT NOT NULL PRIMARY KEY,
            post_id TEXT NOT NULL,
            user_id TEXT NOT NULL,
            display_name TEXT NOT NULL,
            body TEXT NOT NULL,
            sync_status TEXT NOT NULL DEFAULT 'PENDING',
            is_deleted INTEGER NOT NULL DEFAULT 0,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
        """.trimIndent()
    )
    execSQL("CREATE INDEX IF NOT EXISTS index_feed_comment_post_id ON feed_comment(post_id)")
    execSQL("CREATE INDEX IF NOT EXISTS index_feed_comment_user_id ON feed_comment(user_id)")
    execSQL("CREATE INDEX IF NOT EXISTS index_feed_comment_sync_status ON feed_comment(sync_status)")
}

private fun SupportSQLiteDatabase.createFeedHiddenPostTable() {
    execSQL(
        """
        CREATE TABLE IF NOT EXISTS feed_hidden_post (
            post_id TEXT NOT NULL,
            user_id TEXT NOT NULL,
            hidden_at INTEGER NOT NULL,
            PRIMARY KEY(post_id, user_id)
        )
        """.trimIndent()
    )
    execSQL("CREATE INDEX IF NOT EXISTS index_feed_hidden_post_user_id ON feed_hidden_post(user_id)")
}

private fun SupportSQLiteDatabase.hasColumn(tableName: String, columnName: String): Boolean {
    query("PRAGMA table_info(`$tableName`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == columnName) return true
        }
    }
    return false
}
