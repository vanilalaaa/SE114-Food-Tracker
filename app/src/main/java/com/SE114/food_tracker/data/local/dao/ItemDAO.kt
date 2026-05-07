package com.SE114.food_tracker.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.SE114.food_tracker.data.local.entities.Item;

import java.util.List;

@Dao
public interface ItemDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertItem(Item item);

    @Update
    void updateItem(Item item);

    @Delete
    void deleteItem(Item item);

    @Query("SELECT * FROM item ORDER BY created_at DESC")
    LiveData<List<Item>> getAllItems();

    @Query("SELECT * FROM item WHERE category_id = :catId")
    LiveData<List<Item>> getItemsByCategory(int catId);

    @Query("SELECT * FROM item WHERE item_id = :id")
    LiveData<Item> getItemById(long id);

    // Diary: get all items for a specific day (click a day on calendar)
    @Query("SELECT * FROM item WHERE entry_date >= :startOfDay AND entry_date < :endOfDay ORDER BY entry_date DESC")
    LiveData<List<Item>> getItemsByDay(long startOfDay, long endOfDay);

    // Statistics / Spending: flexible date range query
    @Query("SELECT * FROM item WHERE entry_date >= :startDate AND entry_date < :endDate ORDER BY entry_date DESC")
    LiveData<List<Item>> getItemsByDateRange(long startDate, long endDate);

    // Statistics / Spending: same but also filtered by category
    @Query("SELECT * FROM item WHERE entry_date >= :startDate AND entry_date < :endDate AND category_id = :categoryId ORDER BY entry_date DESC")
    LiveData<List<Item>> getItemsByDateRangeAndCategory(long startDate, long endDate, int categoryId);

    // Diary: count items + sum price for a specific day (calendar highlight)
    @Query("SELECT COUNT(*) FROM item WHERE entry_date >= :startOfDay AND entry_date < :endOfDay")
    LiveData<Integer> getItemCountForDay(long startOfDay, long endOfDay);

    @Query("SELECT SUM(price) FROM item WHERE entry_date >= :startOfDay AND entry_date < :endOfDay")
    LiveData<Double> getTotalExpenseForDay(long startOfDay, long endOfDay);
}
