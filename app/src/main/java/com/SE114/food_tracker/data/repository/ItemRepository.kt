package com.SE114.food_tracker.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.SE114.food_tracker.data.local.AppDatabase;
import com.SE114.food_tracker.data.local.dao.ItemDAO;
import com.SE114.food_tracker.data.local.entities.Item;

import java.util.List;

public class ItemRepository {
    private final ItemDAO itemDAO;

    public ItemRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        itemDAO = database.itemDAO();
    }

    // Read
    public LiveData<List<Item>> getAllItems() {
        return itemDAO.getAllItems();
    }
    public LiveData<List<Item>> getItemsByCategory(int categoryId) {
        return itemDAO.getItemsByCategory(categoryId);
    }

    public LiveData<Item> getItemById(long id) {
        return itemDAO.getItemById(id);
    }
    // Write
    public void insert(Item item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            itemDAO.insertItem(item);
        });
    }
    public void update(Item item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            itemDAO.updateItem(item);
        });
    }
    public void delete(Item item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            itemDAO.deleteItem(item);
        });
    }
    // Add Date-range methods
    public LiveData<List<Item>> getItemsByDay(long startOfDay, long endOfDay) {
        return itemDAO.getItemsByDay(startOfDay, endOfDay);
    }
    public LiveData<List<Item>> getItemsByDateRange(long startDate, long endDate) {
        return itemDAO.getItemsByDateRange(startDate, endDate);
    }
    public LiveData<List<Item>> getItemsByDateRangeAndCategory(long startDate, long endDate, int categoryId) {
        return itemDAO.getItemsByDateRangeAndCategory(startDate, endDate, categoryId);
    }
    public LiveData<Integer> getItemCountForDay(long startOfDay, long endOfDay) {
        return itemDAO.getItemCountForDay(startOfDay, endOfDay);
    }
    public LiveData<Double> getTotalExpenseForDay(long startOfDay, long endOfDay){
        return itemDAO.getTotalExpenseForDay(startOfDay, endOfDay);
    }
}
