package com.SE114.food_tracker.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.SE114.food_tracker.data.local.AppDatabase
import com.SE114.food_tracker.data.local.dao.BudgetDAO
import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.dao.ChatDAO
import com.SE114.food_tracker.data.local.dao.FeedDAO
import com.SE114.food_tracker.data.local.dao.ItemDAO
import com.SE114.food_tracker.data.local.dao.FriendDAO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
            .addMigrations(AppDatabase.MIGRATION_8_9)
            .addMigrations(AppDatabase.MIGRATION_9_10)
            .addMigrations(AppDatabase.MIGRATION_10_11)
            .addMigrations(AppDatabase.MIGRATION_11_12)
            .addMigrations(AppDatabase.MIGRATION_12_13)
            .addMigrations(AppDatabase.MIGRATION_13_14)
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onCreate(db)
                    val now = System.currentTimeMillis()

                    db.execSQL("""
                    INSERT INTO category (category_id, owner_id, name, icon_url, is_hidden, is_system, is_deleted, sync_status, created_at, updated_at)
                    VALUES
                    ('a1000000-0000-0000-0000-000000000001', null, 'Cơm',         '🍚', 0, 1, 0, 'PENDING', $now, $now),
                    ('a1000000-0000-0000-0000-000000000002', null, 'Mì & Phở',    '🍜', 0, 1, 0, 'PENDING', $now, $now),
                    ('a1000000-0000-0000-0000-000000000003', null, 'Bánh mì',     '🥖', 0, 1, 0, 'PENDING', $now, $now),
                    ('a1000000-0000-0000-0000-000000000004', null, 'Đồ uống',     '🥤', 0, 1, 0, 'PENDING', $now, $now),
                    ('a1000000-0000-0000-0000-000000000005', null, 'Tráng miệng', '🍰', 0, 1, 0, 'PENDING', $now, $now),
                    ('a1000000-0000-0000-0000-000000000006', null, 'Ăn vặt',      '🍡', 0, 1, 0, 'PENDING', $now, $now);
                    """.trimIndent())
                }
            })
            .build()

    @Provides @Singleton
    fun provideCategoryDao(db: AppDatabase): CategoryDAO = db.categoryDAO()

    @Provides @Singleton
    fun provideItemDao(db: AppDatabase): ItemDAO = db.itemDAO()

    @Provides @Singleton
    fun provideBudgetDao(db: AppDatabase): BudgetDAO = db.budgetDAO()

    @Provides @Singleton
    fun provideFriendDao(db: AppDatabase): FriendDAO = db.friendDao()

    @Provides @Singleton
    fun provideChatDao(db: AppDatabase): ChatDAO = db.chatDao()

    @Provides @Singleton
    fun provideFeedDao(db: AppDatabase): FeedDAO = db.feedDao()
}
