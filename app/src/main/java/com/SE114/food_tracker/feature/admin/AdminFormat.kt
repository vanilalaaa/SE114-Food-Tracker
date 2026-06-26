package com.SE114.food_tracker.feature.admin

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.SE114.food_tracker.R

/** "2026-06-24T10:30:00+00:00" → "2026-06-24 10:30"; leaves anything unexpected untouched. */
internal fun formatReportTime(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return raw.take(16).replace('T', ' ')
}

/**
 * Localized label for a report's stored reason. Mirrors the remote values produced by the report
 * feature's ReportReason enum (kept in sync by value, not imported — admin must not depend on a
 * sibling feature). "other: <free text>" keeps the admin-entered detail; unknown values show as-is.
 */
@Composable
internal fun reasonLabel(reason: String): String {
    if (reason == "other" || reason.startsWith("other:")) {
        val details = reason.substringAfter(':', "").trim()
        val other = stringResource(R.string.report_reason_other)
        return if (details.isEmpty()) other else "$other: $details"
    }
    val res = when (reason) {
        "post_spam_ads" -> R.string.report_reason_post_spam_ads
        "post_sensitive_content" -> R.string.report_reason_post_sensitive_content
        "post_disordered_eating" -> R.string.report_reason_post_disordered_eating
        "post_bullying_defamation" -> R.string.report_reason_post_bullying_defamation
        "user_impersonation" -> R.string.report_reason_user_impersonation
        "user_bad_avatar_name" -> R.string.report_reason_user_bad_avatar_name
        "user_repeated_friend_requests" -> R.string.report_reason_user_repeated_friend_requests
        "message_money_scam" -> R.string.report_reason_message_money_scam
        "message_harassment" -> R.string.report_reason_message_harassment
        "message_malicious_links" -> R.string.report_reason_message_malicious_links
        // Legacy values from reports filed before the reason set was expanded.
        "spam" -> R.string.report_reason_spam
        "inappropriate" -> R.string.report_reason_inappropriate
        "harassment" -> R.string.report_reason_harassment
        else -> null
    }
    return res?.let { stringResource(it) } ?: reason
}

/** Human label for a current ban window: an expiry date, or "vĩnh viễn" when there is none. */
@Composable
internal fun bannedUntilLabel(bannedUntil: String?): String =
    if (bannedUntil.isNullOrBlank()) {
        stringResource(R.string.admin_ban_duration_permanent)
    } else {
        stringResource(R.string.admin_ban_until, formatReportTime(bannedUntil))
    }

/** Localized label for a report status. */
@Composable
internal fun reportStatusLabel(status: String): String = when (status) {
    "pending" -> stringResource(R.string.admin_report_status_pending)
    "resolved" -> stringResource(R.string.admin_report_status_resolved)
    "dismissed" -> stringResource(R.string.admin_report_status_dismissed)
    else -> status
}
