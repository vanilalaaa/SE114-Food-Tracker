package com.SE114.food_tracker.feature.report

import androidx.annotation.StringRes
import com.SE114.food_tracker.R

enum class ReportReasonGroup(@StringRes val labelResId: Int) {
    POST_OR_DIARY(R.string.report_group_post_diary),
    USER(R.string.report_group_user),
    MESSAGE_OR_WALLET(R.string.report_group_message_wallet),
    OTHER(R.string.report_group_other)
}

enum class ReportReason(
    val group: ReportReasonGroup,
    @StringRes val labelResId: Int,
    val remoteValue: String,
    val requiresDetails: Boolean = false
) {
    POST_SPAM_ADS(
        group = ReportReasonGroup.POST_OR_DIARY,
        labelResId = R.string.report_reason_post_spam_ads,
        remoteValue = "post_spam_ads"
    ),
    POST_SENSITIVE_CONTENT(
        group = ReportReasonGroup.POST_OR_DIARY,
        labelResId = R.string.report_reason_post_sensitive_content,
        remoteValue = "post_sensitive_content"
    ),
    POST_DISORDERED_EATING(
        group = ReportReasonGroup.POST_OR_DIARY,
        labelResId = R.string.report_reason_post_disordered_eating,
        remoteValue = "post_disordered_eating"
    ),
    POST_BULLYING_DEFAMATION(
        group = ReportReasonGroup.POST_OR_DIARY,
        labelResId = R.string.report_reason_post_bullying_defamation,
        remoteValue = "post_bullying_defamation"
    ),
    USER_IMPERSONATION(
        group = ReportReasonGroup.USER,
        labelResId = R.string.report_reason_user_impersonation,
        remoteValue = "user_impersonation"
    ),
    USER_BAD_AVATAR_NAME(
        group = ReportReasonGroup.USER,
        labelResId = R.string.report_reason_user_bad_avatar_name,
        remoteValue = "user_bad_avatar_name"
    ),
    USER_REPEATED_FRIEND_REQUESTS(
        group = ReportReasonGroup.USER,
        labelResId = R.string.report_reason_user_repeated_friend_requests,
        remoteValue = "user_repeated_friend_requests"
    ),
    MESSAGE_MONEY_SCAM(
        group = ReportReasonGroup.MESSAGE_OR_WALLET,
        labelResId = R.string.report_reason_message_money_scam,
        remoteValue = "message_money_scam"
    ),
    MESSAGE_HARASSMENT(
        group = ReportReasonGroup.MESSAGE_OR_WALLET,
        labelResId = R.string.report_reason_message_harassment,
        remoteValue = "message_harassment"
    ),
    MESSAGE_MALICIOUS_LINKS(
        group = ReportReasonGroup.MESSAGE_OR_WALLET,
        labelResId = R.string.report_reason_message_malicious_links,
        remoteValue = "message_malicious_links"
    ),
    OTHER(
        group = ReportReasonGroup.OTHER,
        labelResId = R.string.report_reason_other,
        remoteValue = "other",
        requiresDetails = true
    );

    fun toRemoteValue(details: String?): String =
        details
            ?.trim()
            ?.takeIf { requiresDetails && it.isNotBlank() }
            ?.let { "$remoteValue: $it" }
            ?: remoteValue
}
