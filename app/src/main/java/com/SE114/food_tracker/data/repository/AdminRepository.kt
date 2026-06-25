package com.SE114.food_tracker.data.repository

/** Counters for the admin dashboard, from `admin_dashboard_stats`. */
data class AdminDashboardStats(
    val totalUsers: Long,
    val bannedCount: Long,
    val deletedCount: Long,
    val pendingReports: Long
)

/** A user row as returned by `admin_list_users` (includes admin-only flags). */
data class AdminUser(
    val id: String,
    val displayName: String?,
    val userId: String?,
    val avatarUrl: String?,
    val isAdmin: Boolean,
    val isBanned: Boolean,
    val isDeleted: Boolean
)

/** A report joined with reporter/target handles, from `admin_list_reports`. */
data class AdminReport(
    val id: String,
    val reporterId: String,
    val reporterHandle: String?,
    val targetId: String,
    val targetHandle: String?,
    val reason: String,
    val status: String,
    val createdAt: String?
)

/**
 * Admin-only operations. Every method calls a `security definer` RPC that re-checks
 * `is_admin` server-side — never direct table access — so a non-admin who reaches the
 * UI is rejected with [AuthError.NotAuthorized].
 */
interface AdminRepository {
    suspend fun dashboardStats(): AuthOutcome<AdminDashboardStats>
    suspend fun listUsers(search: String, limit: Int, offset: Int): AuthOutcome<List<AdminUser>>
    suspend fun setBanned(targetId: String, banned: Boolean): AuthOutcome<Unit>
    suspend fun setDeleted(targetId: String, deleted: Boolean): AuthOutcome<Unit>

    /** Grant or revoke admin rights on [targetId]. */
    suspend fun setAdmin(targetId: String, admin: Boolean): AuthOutcome<Unit>
    suspend fun listReports(status: String, limit: Int, offset: Int): AuthOutcome<List<AdminReport>>

    /** [status] must be `"resolved"` or `"dismissed"`. */
    suspend fun resolveReport(reportId: String, status: String): AuthOutcome<Unit>
}
