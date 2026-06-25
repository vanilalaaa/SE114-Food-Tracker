package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.data.remote.dto.ReportDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ReportRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    suspend fun submitProfileReport(
        targetId: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val reporterId = supabaseClient.auth.currentUserOrNull()?.id
                ?: error("Chưa đăng nhập")

            if (reporterId == targetId) {
                error("Không thể báo cáo chính mình.")
            }

            supabaseClient.postgrest.from("report").insert(
                ReportDTO(
                    reporterId = reporterId,
                    targetId = targetId,
                    reason = reason
                )
            )
            Unit
        }
    }
}
