package com.SE114.food_tracker.data.local.entities

// Trạng thái đồng bộ của dữ liệu giữa Local và Server
enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED
}
