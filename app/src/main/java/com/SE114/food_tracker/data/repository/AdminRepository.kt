package com.SE114.food_tracker.data.repository

data class AdminDashboardStats(
    val totalUsers: Long,
    val bannedCount: Long,
    val deletedCount: Long,
    val pendingReports: Long
)

/**
 * A user row as returned by `admin_list_users` (includes admin-only flags).
 *
 * [isBanned] is "currently banned" (permanent or a not-yet-expired temporary ban).
 * [bannedUntil] is the expiry of a temporary ban (null = permanent or not banned).
 * [banCount] is the lifetime number of bans applied to the account.
 * [lastBannedAt] is the ISO-8601 timestamp of the most recent ban (null if never banned).
 * [deletionExpiresAt] is the ISO-8601 timestamp after which the soft-deleted account
 * can no longer be restored (null when not deleted).
 */
data class AdminUser(
    val id: String,
    val displayName: String?,
    val userId: String?,
    val avatarUrl: String?,
    val isAdmin: Boolean,
    val isBanned: Boolean,
    val isDeleted: Boolean,
    val bannedUntil: String? = null,
    val banCount: Int = 0,
    val lastBannedAt: String? = null,
    val deletionExpiresAt: String? = null
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
    val createdAt: String?,
    val targetBanCount: Int = 0,
    val targetBannedUntil: String? = null
)

/**
 * Admin-only operations. Every method calls a `security definer` RPC that re-checks
 * `is_admin` server-side — never direct table access — so a non-admin who reaches the
 * UI is rejected with [AuthError.NotAuthorized].
 */
interface AdminRepository {
    suspend fun dashboardStats(): AuthOutcome<AdminDashboardStats>
    suspend fun listUsers(search: String, limit: Int, offset: Int): AuthOutcome<List<AdminUser>>

    /**
     * Ban or unban [targetId].
     *
     * When [banned] is true, [durationSeconds] sets a temporary ban (null = permanent);
     * when false it lifts the ban ([durationSeconds] ignored).
     *
     * A successful ban also triggers the `notify-ban` email (best-effort).
     *
     * Fails with:
     * - [AuthError.SelfAction] if [targetId] is the caller's own account.
     * - [AuthError.TargetIsAdmin] if [targetId] belongs to another admin.
     * * *(Note: Permanent bans no longer have a 30-day irrevocable limit and can be lifted at any time).*
     */
    suspend fun setBanned(targetId: String, banned: Boolean, durationSeconds: Long?): AuthOutcome<Unit>

    /**
     * Soft-delete or restore [targetId].
     *
     * On soft-delete a 30-day restore window is set server-side. Restore is refused
     * with [AuthError.DeletionExpired] once that window has elapsed. After 30 days,
     * the account is permanently purged from the database.
     *
     * Fails with:
     * - [AuthError.SelfAction] if [targetId] is the caller's own account.
     * - [AuthError.TargetIsAdmin] if [targetId] belongs to another admin.
     */
    suspend fun setDeleted(targetId: String, deleted: Boolean): AuthOutcome<Unit>

    /**
     * Grant or revoke admin rights on [targetId].
     *
     * Fails with:
     * - [AuthError.SelfAction] if [targetId] is the caller's own account (an admin cannot change their own role).
     * - [AuthError.BannedUserCannotBeAdmin] if attempting to grant admin status to a permanently banned user.
     */
    suspend fun setAdmin(targetId: String, admin: Boolean): AuthOutcome<Unit>

    suspend fun listReports(status: String, limit: Int, offset: Int): AuthOutcome<List<AdminReport>>

    /** [status] must be `"resolved"` or `"dismissed"`. */
    suspend fun resolveReport(reportId: String, status: String): AuthOutcome<Unit>
}