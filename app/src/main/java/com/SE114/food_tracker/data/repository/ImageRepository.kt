package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.data.remote.SupabaseClient
import io.github.jan.supabase.storage.storage

class ImageRepository {

    suspend fun uploadItemImage(userId: String, itemId: String, bytes: ByteArray): Result<String> {
        return try {
            val path = "$userId/$itemId.jpg"

            // Tiến hành upload đè (upsert = true) lên bucket "items"
            SupabaseClient.client.storage.from("items").upload(path, bytes) {
                upsert = true
            }

            // Lấy URL công khai (Public URL) để lưu vào database local/remote
            val publicUrl = SupabaseClient.client.storage.from("items").publicUrl(path)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Xóa ảnh món ăn trên Supabase Storage thông qua đường dẫn cấu trúc <user_id>/<item_id>.jpg
    suspend fun deleteItemImage(userId: String, itemId: String): Result<Unit> {
        return try {
            val path = "$userId/$itemId.jpg"
            SupabaseClient.client.storage.from("items").delete(listOf(path))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}