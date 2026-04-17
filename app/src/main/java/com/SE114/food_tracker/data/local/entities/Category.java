package com.SE114.food_tracker.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "category")
public class Category {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "category_id")
    private int categoryId;
    private String name;
    @ColumnInfo(name = "icon_url")
    private String iconUrl;
    @ColumnInfo(name = "is_hidden")
    private boolean isHidden;
    @ColumnInfo(name = "is_system")
    private boolean isSystem;

    public Category(String name, String iconUrl, boolean isHidden, boolean isSystem) {
        this.name = name;
        this.iconUrl = iconUrl;
        this.isHidden = isHidden;
        this.isSystem = isSystem;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public void setSystem(boolean system) {
        isSystem = system;
    }
}
