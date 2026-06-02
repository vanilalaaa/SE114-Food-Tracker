package com.SE114.food_tracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.SE114.food_tracker.data.local.dao.BudgetDAO
import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.dao.ItemDAO
import com.SE114.food_tracker.data.local.entities.Budget
import com.SE114.food_tracker.data.local.entities.Category
import com.SE114.food_tracker.data.local.entities.Item

@Database(
    entities = [Category::class, Item::class, Budget::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDAO(): CategoryDAO
    abstract fun itemDAO(): ItemDAO
    abstract fun budgetDAO(): BudgetDAO // Khai báo abstract fun cho BudgetDAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    // Xóa database cũ để tạo lại bảng theo cấu hình mới, tránh crash do đổi kiểu dữ liệu ID
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}