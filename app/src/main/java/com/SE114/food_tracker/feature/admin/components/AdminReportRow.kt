package com.SE114.food_tracker.feature.admin.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.R
import com.SE114.food_tracker.data.repository.AdminReport
import com.SE114.food_tracker.feature.admin.formatReportTime
import com.SE114.food_tracker.feature.admin.reasonLabel
import com.SE114.food_tracker.feature.admin.reportStatusLabel

@Composable
fun AdminReportRow(
    report: AdminReport,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string.admin_report_target,
                    report.targetHandle ?: report.targetId
                ),
                style = MaterialTheme.typography.titleSmall
            )
            ReportStatusBadge(status = report.status)
        }
        Text(
            text = stringResource(
                R.string.admin_report_reporter,
                report.reporterHandle ?: report.reporterId
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.admin_report_reason, reasonLabel(report.reason)),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = formatReportTime(report.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReportStatusBadge(status: String) {
    val (container, content) = when (status) {
        "resolved" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "dismissed" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    AdminStatusBadge(text = reportStatusLabel(status), container = container, content = content)
}
