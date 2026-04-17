package com.SE114.food_tracker.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.SE114.food_tracker.data.local.dao.CategoryDAO;
import com.SE114.food_tracker.data.local.dao.ItemDAO;
import com.SE114.food_tracker.data.local.entities.Category;
import com.SE114.food_tracker.data.local.entities.Item;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Category.class, Item.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CategoryDAO categoryDAO();
    public abstract ItemDAO itemDAO();
    private static volatile AppDatabase instance;

    public static final int Number_of_threads = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(Number_of_threads);

    public static AppDatabase getInstance(final Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "app_database")
                            .build();
                }
            }
        }
        return instance;
    }
}
