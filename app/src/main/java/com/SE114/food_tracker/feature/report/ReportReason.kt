package com.SE114.food_tracker.feature.report

import androidx.annotation.StringRes
import com.SE114.food_tracker.R

enum class ReportReason(
    @StringRes val labelResId: Int,
    val remoteValue: String
) {
    SPAM(
        labelResId = R.string.report_reason_spam,
        remoteValue = "spam"
    ),
    INAPPROPRIATE(
        labelResId = R.string.report_reason_inappropriate,
        remoteValue = "inappropriate"
    ),
    HARASSMENT(
        labelResId = R.string.report_reason_harassment,
        remoteValue = "harassment"
    ),
    OTHER(
        labelResId = R.string.report_reason_other,
        remoteValue = "other"
    )
}
