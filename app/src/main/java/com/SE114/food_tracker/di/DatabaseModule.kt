package com.SE114.food_tracker.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.SE114.food_tracker.data.local.AppDatabase
import com.SE114.food_tracker.data.local.dao.BudgetDAO
import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.dao.ItemDAO
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
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onCreate(db)

                    val now = System.currentTimeMillis()

                    db.execSQL("""
                        INSERT INTO category (category_id, owner_id, name, icon_url, is_hidden, is_system, sync_status, created_at, updated_at)
                        VALUES 
                        ('sys-cat-1', null, 'Cơm', '🍚', 0, 1, 'PENDING', $now, $now),
                        ('sys-cat-2', null, 'Mì & Phở', '🍜', 0, 1, 'PENDING', $now, $now),
                        ('sys-cat-3', null, 'Bánh mì', '🥖', 0, 1, 'PENDING', $now, $now),
                        ('sys-cat-4', null, 'Đồ uống', '🥤', 0, 1, 'PENDING', $now, $now),
                        ('sys-cat-5', null, 'Tráng miệng', '🍰', 0, 1, 'PENDING', $now, $now),
                        ('sys-cat-6', null, 'Ăn vặt', '🍡', 0, 1, 'PENDING', $now, $now);
                    """.trimIndent())
                }
            })
            .build()

    @Provides
    @Singleton
    fun provideCategoryDao(db: AppDatabase): CategoryDAO = db.categoryDAO()

    @Provides
    @Singleton
    fun provideItemDao(db: AppDatabase): ItemDAO = db.itemDAO()

    @Provides
    @Singleton
    fun provideBudgetDao(db: AppDatabase): BudgetDAO = db.budgetDAO()
}
