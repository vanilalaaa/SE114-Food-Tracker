package com.SE114.food_tracker.feature.report

enum class ReportReason(
    val label: String,
    val remoteValue: String
) {
    SPAM(
        label = "Spam",
        remoteValue = "spam"
    ),
    INAPPROPRIATE(
        label = "Nội dung không phù hợp",
        remoteValue = "inappropriate"
    ),
    HARASSMENT(
        label = "Quấy rối",
        remoteValue = "harassment"
    ),
    OTHER(
        label = "Khác",
        remoteValue = "other"
    )
}
