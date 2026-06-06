package com.SE114.food_tracker.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.SE114.food_tracker.data.repository.ItemRepository
import com.SE114.food_tracker.data.repository.CategoryRepository
import com.SE114.food_tracker.data.remote.SupabaseItemService
import com.SE114.food_tracker.data.remote.mapper.DataMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import com.SE114.food_tracker.data.remote.dto.CategoryDTO
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

@HiltWorker
class Sync @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val supabaseItemService: SupabaseItemService,
    private val supabaseClient: SupabaseClient
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.d("SyncWorker bắt đầu chạy...")

        if (!isDeviceOnline()) {
            Timber.w("Thiết bị offline. Đặt lịch chạy lại khi có mạng.")
            return Result.retry()
        }

        val ownerId = supabaseClient.auth.currentUserOrNull()?.id
        if (ownerId == null) {
            Timber.w("Chưa nhận được Auth Session. Có thể do hàm signIn ngầm chưa xong. Yêu cầu retry...")
            return Result.retry()
        }
        Timber.d("Xác thực thành công. User UUID: $ownerId")

        var insideJobError = false

        // BƯỚC 1.1: ĐẨY CATEGORY (CHƯA ĐỒNG BỘ) LÊN SERVER
        try {
            val pendingCategories = categoryRepository.getPendingCategories()
            for (category in pendingCategories) {
                val dto = with(DataMapper) { category.toDto() }

                runCatching {
                    supabaseClient.postgrest.from("category").upsert(dto)
                }.onSuccess {
                    categoryRepository.markSynced(category.categoryId)
                    Timber.d("Đồng bộ thành công danh mục: ${category.name}")
                }.onFailure { error ->
                    Timber.e(error, "Lỗi khi đẩy danh mục ${category.name}")
                    insideJobError = true
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Gặp lỗi nghiêm trọng trong luồng đẩy Category")
            insideJobError = true
        }

        // BƯỚC 1.2: ĐẨY ITEM (CHƯA ĐỒNG BỘ) LÊN SERVER
        try {
            val pendingItems = itemRepository.getPendingItems()
            for (item in pendingItems) {
                supabaseItemService.uploadItem(item, ownerId)
                    .onSuccess {
                        itemRepository.markSynced(item.itemId)
                        Timber.d("Đồng bộ thành công món ăn: ${item.name}")
                    }
                    .onFailure { error ->
                        Timber.e(error, "Lỗi khi đẩy món ăn ${item.name}")
                        itemRepository.markFailed(item.itemId)
                        insideJobError = true
                    }
            }
        } catch (e: Exception) {
            Timber.e(e, "Gặp lỗi nghiêm trọng trong luồng đẩy Item")
            insideJobError = true
        }

        // BƯỚC 2.1: KÉO CATEGORY (HỆ THỐNG + CỦA USER) TỪ SERVER VỀ MÁY
        try {
            val remoteCategories = supabaseClient.postgrest.from("category")
                .select {
                    filter {
                        or {
                            eq("owner_id", ownerId)
                            eq("is_system", true)
                        }
                    }
                }.decodeList<CategoryDTO>()

            if (remoteCategories.isNotEmpty()) {
                val entities = remoteCategories.map { with(DataMapper) { it.toEntity() } }
                categoryRepository.upsertCategoriesFromServer(entities)
                Timber.d("Đã kéo và cập nhật ${remoteCategories.size} danh mục từ Server.")
            }
        } catch (e: Exception) {
            Timber.e(e, "Lỗi khi kéo danh mục từ Server")
            insideJobError = true
        }

        // BƯỚC 2.2: KÉO ITEM (TÍNH TỪ THỜI ĐIỂM CẬP NHẬT MỚI NHẤT) VỀ MÁY
        try {
            val allLocalItems = itemRepository.getAllItems().firstOrNull().orEmpty()
            val maxLocalUpdatedAt = allLocalItems.maxOfOrNull { it.updatedAt } ?: 0L

            supabaseItemService.fetchItemsSince(ownerId, maxLocalUpdatedAt)
                .onSuccess { remoteDtos ->
                    if (remoteDtos.isNotEmpty()) {
                        val entitiesToUpsert = remoteDtos.map { with(DataMapper) { it.toEntity() } }
                        itemRepository.upsertItemsFromServer(entitiesToUpsert)
                        Timber.d("Đã kéo và cập nhật ${remoteDtos.size} món ăn mới từ Server về máy.")
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Lỗi khi kéo món ăn từ Server")
                    insideJobError = true
                }
        } catch (e: Exception) {
            Timber.e(e, "Gặp lỗi nghiêm trọng trong luồng kéo dữ liệu")
            insideJobError = true
        }

        return if (insideJobError) Result.retry() else Result.success()
    }

    private fun isDeviceOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}