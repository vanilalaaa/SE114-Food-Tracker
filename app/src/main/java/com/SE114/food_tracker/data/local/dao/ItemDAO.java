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
}
