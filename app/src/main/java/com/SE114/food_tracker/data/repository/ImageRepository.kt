package com.SE114.food_tracker.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ImageRepository {
    private val storage = FirebaseStorage.getInstance()
    private val imagesRef = storage.reference.child("images")

    // Tải ảnh lên và trả về Result (thành công chứa URL, thất bại chứa lỗi)
    suspend fun uploadImage(imageBytes: ByteArray): Result<String> {
        return try {
            val fileName = UUID.randomUUID().toString()
            val fileRef = imagesRef.child("$fileName.jpg")
            fileRef.putBytes(imageBytes).await()
            val downloadUrl = fileRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Xóa ảnh trên Firebase bằng URL
    suspend fun deleteImage(imageUrl: String?): Result<Unit> {
        if (imageUrl.isNullOrBlank()) return Result.success(Unit)
        return try {
            storage.getReferenceFromUrl(imageUrl).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
