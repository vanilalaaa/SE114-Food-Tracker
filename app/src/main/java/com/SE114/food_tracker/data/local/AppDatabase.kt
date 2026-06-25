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
        FeedComment::class
    ],
    version = 14,
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
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE category ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.createFeedTables()
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE item ADD COLUMN owner_id TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_item_owner_id ON item(owner_id)")
                // Rows created before owner_id are unscoped cross-account leftovers — the exact
                // bug this migration fixes — and have no reliable owner. Deleting them is
                // intentional: the correct per-user data re-pulls from Supabase on next login.
                // System categories (owner_id NULL) are preserved.
                db.execSQL("DELETE FROM item")
                db.execSQL("DELETE FROM category WHERE is_system = 0")
                db.execSQL("DELETE FROM budget")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE feed_comment ADD COLUMN parent_comment_id TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_feed_comment_parent_comment_id ON feed_comment(parent_comment_id)")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile_cache ADD COLUMN profile_user_id TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE conversations ADD COLUMN last_message_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE conversations ADD COLUMN last_message_snippet TEXT")

                db.execSQL("ALTER TABLE conversation_participants ADD COLUMN last_read_at INTEGER NOT NULL DEFAULT 0")
            }
        }
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
            parent_comment_id TEXT,
            sync_status TEXT NOT NULL DEFAULT 'PENDING',
            is_deleted INTEGER NOT NULL DEFAULT 0,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
        """.trimIndent()
    )
    execSQL("CREATE INDEX IF NOT EXISTS index_feed_comment_post_id ON feed_comment(post_id)")
    execSQL("CREATE INDEX IF NOT EXISTS index_feed_comment_parent_comment_id ON feed_comment(parent_comment_id)")
    execSQL("CREATE INDEX IF NOT EXISTS index_feed_comment_user_id ON feed_comment(user_id)")
    execSQL("CREATE INDEX IF NOT EXISTS index_feed_comment_sync_status ON feed_comment(sync_status)")
}
