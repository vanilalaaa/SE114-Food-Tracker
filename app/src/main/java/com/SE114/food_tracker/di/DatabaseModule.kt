package com.SE114.food_tracker.di

import android.content.Context
import androidx.room.Room
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
