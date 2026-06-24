package com.SE114.food_tracker.feature.admin

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.SE114.food_tracker.R

/** "2026-06-24T10:30:00+00:00" → "2026-06-24 10:30"; leaves anything unexpected untouched. */
internal fun formatReportTime(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return raw.take(16).replace('T', ' ')
}

/** Localized label for a report reason remote value; unknown values show as-is. */
@Composable
internal fun reasonLabel(reason: String): String = when (reason) {
    "spam" -> stringResource(R.string.report_reason_spam)
    "inappropriate" -> stringResource(R.string.report_reason_inappropriate)
    "harassment" -> stringResource(R.string.report_reason_harassment)
    "other" -> stringResource(R.string.report_reason_other)
    else -> reason
}

/** Localized label for a report status. */
@Composable
internal fun reportStatusLabel(status: String): String = when (status) {
    "pending" -> stringResource(R.string.admin_report_status_pending)
    "resolved" -> stringResource(R.string.admin_report_status_resolved)
    "dismissed" -> stringResource(R.string.admin_report_status_dismissed)
    else -> status
}
