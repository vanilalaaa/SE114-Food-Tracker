package com.SE114.food_tracker.data.repository;
import android.app.Application;

import androidx.lifecycle.LiveData;

import com.SE114.food_tracker.data.local.AppDatabase;
import com.SE114.food_tracker.data.local.dao.CategoryDAO;
import com.SE114.food_tracker.data.local.entities.Category;

import java.util.List;

public class CategoryRepository {
    private final CategoryDAO categoryDAO;

    public CategoryRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        categoryDAO = database.categoryDAO();
    }

    // Read
    public LiveData<List<Category>> getAllCategories() {
        return categoryDAO.getAllCategories();
    }
    // Write
    public void insert(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categoryDAO.insert(category);
        });
    }
    public void update(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categoryDAO.update(category);
        });
    }
    public void delete(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categoryDAO.delete(category);
        });
    }
}
