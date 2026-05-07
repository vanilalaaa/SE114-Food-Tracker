package com.SE114.food_tracker.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "item",
        foreignKeys = @ForeignKey(entity = Category.class,
                parentColumns = "category_id",
                childColumns = "category_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("category_id")}
)
public class Item {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "item_id")
    private int itemId;
    @ColumnInfo(name = "category_id")
    private int categoryId;
    private String name;
    @ColumnInfo(name = "time_type")
    private int timeType;
    private double price;
    private Integer rating;
    private String note;
    @ColumnInfo(name = "image_path")
    private String imagePath;
    @ColumnInfo(name = "is_synced")
    private String isSynced;
    @ColumnInfo(name = "entry_date")
    private long entryDate;
    @ColumnInfo(name = "created_at")
    private long createdAt;

    public Item() {
        this.createdAt = System.currentTimeMillis();
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
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

    public int getTimeType() {
        return timeType;
    }

    public void setTimeType(int timeType) {
        this.timeType = timeType;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getIsSynced() {
        return isSynced;
    }

    public void setIsSynced(String isSynced) {
        this.isSynced = isSynced;
    }

    public long getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(long entryDate) {
        this.entryDate = entryDate;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
