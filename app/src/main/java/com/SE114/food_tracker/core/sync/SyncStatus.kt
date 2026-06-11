package com.SE114.food_tracker.core.sync

// Trạng thái đồng bộ của một bản ghi giữa Room và Supabase.
enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED
}
