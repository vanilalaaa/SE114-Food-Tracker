package com.SE114.food_tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.SE114.food_tracker.data.local.dao.BudgetDAO
import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.dao.ItemDAO
import com.SE114.food_tracker.data.local.entities.Budget
import com.SE114.food_tracker.data.local.entities.Category
import com.SE114.food_tracker.data.local.entities.Item
import com.SE114.food_tracker.data.local.entities.Conversation
import com.SE114.food_tracker.data.local.entities.ConversationParticipant
import com.SE114.food_tracker.data.local.entities.Message
import com.SE114.food_tracker.data.local.dao.ChatDAO

@Database(
    entities = [Category::class, Item::class, Budget::class, Conversation::class, ConversationParticipant::class, Message::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDAO(): CategoryDAO
    abstract fun itemDAO(): ItemDAO
    abstract fun budgetDAO(): BudgetDAO
    abstract fun chatDao(): ChatDAO
}