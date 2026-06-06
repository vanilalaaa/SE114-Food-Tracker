package com.SE114.food_tracker.data.remote

import com.SE114.food_tracker.data.local.entities.Item
import com.SE114.food_tracker.data.remote.dto.ItemDTO
import com.SE114.food_tracker.data.remote.mapper.DataMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.datetime.Instant
import javax.inject.Inject

class SupabaseItemService @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    private fun isAuthenticated(): Boolean {
        return supabaseClient.auth.currentSessionOrNull() != null
    }

    //CHIỀU ĐI: Đẩy món ăn lên Supabase sử dụng mapper toDto
    suspend fun uploadItem(item: Item, ownerId: String): Result<Unit> = runCatching {
        if (!isAuthenticated()) throw IllegalStateException("User unauthenticated.")

        // Sử dụng chính xác Mapper cậu đã viết
        val dto: ItemDTO = with(DataMapper) { item.toDto(ownerId) }

        supabaseClient.postgrest.from("item").upsert(dto)
    }

    // CHIỀU ĐI: Xóa món ăn dựa trên RLS chủ sở hữu
    suspend fun deleteItem(itemId: String): Result<Unit> = runCatching {
        if (!isAuthenticated()) throw IllegalStateException("User unauthenticated.")

        supabaseClient.postgrest.from("item").delete {
            filter { eq("id", itemId) }
        }
    }


    // CHIỀU VỀ: Lấy dữ liệu mới từ Server phát sinh sau mốc thời gian local
    suspend fun fetchItemsSince(ownerId: String, sinceTimestamp: Long): Result<List<ItemDTO>> = runCatching {
        if (!isAuthenticated()) throw IllegalStateException("User unauthenticated.")

        val sinceIsoString = Instant.fromEpochMilliseconds(sinceTimestamp).toString()

        supabaseClient.postgrest.from("item")
            .select {
                filter {
                    eq("owner_id", ownerId)
                    gt("updated_at", sinceIsoString)
                }
            }.decodeList<ItemDTO>()
    }
}