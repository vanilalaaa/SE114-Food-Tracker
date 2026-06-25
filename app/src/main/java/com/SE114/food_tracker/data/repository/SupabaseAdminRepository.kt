package com.SE114.food_tracker.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAdminRepository @Inject constructor(
    private val client: SupabaseClient
) : AdminRepository {

    private val db get() = client.postgrest

    @Serializable
    private data class StatsRow(
        @SerialName("total_users") val totalUsers: Long = 0,
        @SerialName("banned_count") val bannedCount: Long = 0,
        @SerialName("deleted_count") val deletedCount: Long = 0,
        @SerialName("pending_reports") val pendingReports: Long = 0
    )

    @Serializable
    private data class UserRow(
        @SerialName("id") val id: String,
        @SerialName("display_name") val displayName: String? = null,
        @SerialName("user_id") val userId: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null,
        @SerialName("is_admin") val isAdmin: Boolean = false,
        @SerialName("is_banned") val isBanned: Boolean = false,
        @SerialName("banned_until") val bannedUntil: String? = null,
        @SerialName("ban_count") val banCount: Int = 0,
        @SerialName("deleted_at") val deletedAt: String? = null,
        @SerialName("created_at") val createdAt: String? = null
    )

    @Serializable
    private data class ReportRow(
        @SerialName("id") val id: String,
        @SerialName("reporter_id") val reporterId: String,
        @SerialName("reporter_user_id") val reporterUserId: String? = null,
        @SerialName("reporter_display_name") val reporterDisplayName: String? = null,
        @SerialName("target_id") val targetId: String,
        @SerialName("target_user_id") val targetUserId: String? = null,
        @SerialName("target_display_name") val targetDisplayName: String? = null,
        @SerialName("target_ban_count") val targetBanCount: Int = 0,
        @SerialName("target_banned_until") val targetBannedUntil: String? = null,
        @SerialName("reason") val reason: String,
        @SerialName("status") val status: String,
        @SerialName("created_at") val createdAt: String? = null
    )

    override suspend fun dashboardStats(): AuthOutcome<AdminDashboardStats> = withContext(Dispatchers.IO) {
        runCatching {
            val row = db.rpc("admin_dashboard_stats").decodeList<StatsRow>().firstOrNull()
                ?: StatsRow()
            AdminDashboardStats(
                totalUsers = row.totalUsers,
                bannedCount = row.bannedCount,
                deletedCount = row.deletedCount,
                pendingReports = row.pendingReports
            )
        }.fold(
            onSuccess = { AuthOutcome.Success(it) },
            onFailure = { AuthOutcome.Failure(it.toAdminError()) }
        )
    }

    override suspend fun listUsers(
        search: String,
        limit: Int,
        offset: Int
    ): AuthOutcome<List<AdminUser>> = withContext(Dispatchers.IO) {
        runCatching {
            db.rpc(
                "admin_list_users",
                buildJsonObject {
                    put("p_search", search)
                    put("p_limit", limit)
                    put("p_offset", offset)
                }
            ).decodeList<UserRow>().map {
                AdminUser(
                    id = it.id,
                    displayName = it.displayName,
                    userId = it.userId,
                    avatarUrl = it.avatarUrl,
                    isAdmin = it.isAdmin,
                    isBanned = it.isBanned,
                    isDeleted = it.deletedAt != null,
                    bannedUntil = it.bannedUntil,
                    banCount = it.banCount
                )
            }
        }.fold(
            onSuccess = { AuthOutcome.Success(it) },
            onFailure = { AuthOutcome.Failure(it.toAdminError()) }
        )
    }

    override suspend fun setBanned(
        targetId: String,
        banned: Boolean,
        durationSeconds: Long?
    ): AuthOutcome<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                db.rpc(
                    "admin_set_ban",
                    buildJsonObject {
                        put("p_target", targetId)
                        put("p_banned", banned)
                        // Omitted for unban / permanent ban → the RPC default (null = permanent).
                        if (banned && durationSeconds != null) put("p_duration_seconds", durationSeconds)
                    }
                )
                Unit
            }.fold(
                onSuccess = {
                    if (banned) notifyBan(targetId, durationSeconds)
                    AuthOutcome.Success(Unit)
                },
                onFailure = { AuthOutcome.Failure(it.toAdminError()) }
            )
        }

    @Serializable
    private data class NotifyBanBody(
        @SerialName("target_id") val targetId: String,
        @SerialName("duration_seconds") val durationSeconds: Long? = null
    )

    // Best-effort ban-notification email via the notify-ban Edge Function. A delivery failure is
    // logged but never fails the ban itself — the account is already banned by the RPC above.
    private suspend fun notifyBan(targetId: String, durationSeconds: Long?) {
        runCatching {
            client.functions.invoke(
                function = "notify-ban",
                body = NotifyBanBody(targetId, durationSeconds),
                headers = Headers.build { append(HttpHeaders.ContentType, "application/json") }
            )
        }.onFailure { Timber.tag("Admin").e(it, "notify-ban email failed") }
    }

    override suspend fun setAdmin(targetId: String, admin: Boolean): AuthOutcome<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                db.rpc(
                    "admin_set_admin",
                    buildJsonObject {
                        put("p_target", targetId)
                        put("p_admin", admin)
                    }
                )
                Unit
            }.fold(
                onSuccess = { AuthOutcome.Success(Unit) },
                onFailure = { AuthOutcome.Failure(it.toAdminError()) }
            )
        }

    override suspend fun setDeleted(targetId: String, deleted: Boolean): AuthOutcome<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                db.rpc(
                    "admin_set_deleted",
                    buildJsonObject {
                        put("p_target", targetId)
                        put("p_deleted", deleted)
                    }
                )
                Unit
            }.fold(
                onSuccess = { AuthOutcome.Success(Unit) },
                onFailure = { AuthOutcome.Failure(it.toAdminError()) }
            )
        }

    override suspend fun listReports(
        status: String,
        limit: Int,
        offset: Int
    ): AuthOutcome<List<AdminReport>> = withContext(Dispatchers.IO) {
        runCatching {
            db.rpc(
                "admin_list_reports",
                buildJsonObject {
                    put("p_status", status)
                    put("p_limit", limit)
                    put("p_offset", offset)
                }
            ).decodeList<ReportRow>().map {
                AdminReport(
                    id = it.id,
                    reporterId = it.reporterId,
                    reporterHandle = it.reporterUserId ?: it.reporterDisplayName,
                    targetId = it.targetId,
                    targetHandle = it.targetUserId ?: it.targetDisplayName,
                    reason = it.reason,
                    status = it.status,
                    createdAt = it.createdAt,
                    targetBanCount = it.targetBanCount,
                    targetBannedUntil = it.targetBannedUntil
                )
            }
        }.fold(
            onSuccess = { AuthOutcome.Success(it) },
            onFailure = { AuthOutcome.Failure(it.toAdminError()) }
        )
    }

    override suspend fun resolveReport(reportId: String, status: String): AuthOutcome<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                db.rpc(
                    "admin_resolve_report",
                    buildJsonObject {
                        put("p_report", reportId)
                        put("p_status", status)
                    }
                )
                Unit
            }.fold(
                onSuccess = { AuthOutcome.Success(Unit) },
                onFailure = { AuthOutcome.Failure(it.toAdminError()) }
            )
        }
}
