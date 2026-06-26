package com.SE114.food_tracker.feature.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.theme.CardWhite
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.StatPinkDark
import com.SE114.food_tracker.core.designsystem.theme.TextLabelGray
import com.SE114.food_tracker.core.designsystem.theme.TextPrimary
import com.SE114.food_tracker.core.designsystem.theme.TextSecondary

@Composable
fun ReportDialog(
    isSubmitting: Boolean,
    onDismissRequest: () -> Unit,
    onConfirmReport: (ReportReason, String?) -> Unit
) {
    var expandedGroupNames by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectedReasonName by rememberSaveable { mutableStateOf<String?>(null) }
    var otherDetails by rememberSaveable { mutableStateOf("") }
    val selectedReason = selectedReasonName?.let(ReportReason::valueOf)
    val canSubmit = !isSubmitting &&
        selectedReason != null &&
        (!selectedReason.requiresDetails || otherDetails.isNotBlank())

    AlertDialog(
        onDismissRequest = {
            if (!isSubmitting) onDismissRequest()
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedReason?.let { reason ->
                        onConfirmReport(reason, otherDetails.takeIf { it.isNotBlank() })
                    }
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = StatPinkDark),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isSubmitting) R.string.report_submitting else R.string.report_submit
                    ),
                    color = CardWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !isSubmitting
            ) {
                Text(stringResource(R.string.report_cancel), color = TextLabelGray)
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.report_dialog_title),
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.report_reason_prompt),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(
                    items = ReportReasonGroup.values().toList(),
                    key = { it.name }
                ) { group ->
                    val isExpanded = group.name in expandedGroupNames

                    ReportGroupHeader(
                        group = group,
                        expanded = isExpanded,
                        enabled = !isSubmitting,
                        onClick = {
                            expandedGroupNames =
                                if (isExpanded) {
                                    expandedGroupNames - group.name
                                } else {
                                    expandedGroupNames + group.name
                                }

                        }
                    )

                    if (isExpanded) {
                        if (group == ReportReasonGroup.OTHER) {
                            OutlinedTextField(
                                value = otherDetails,
                                onValueChange = {
                                    otherDetails = it
                                },
                                enabled = !isSubmitting,
                                placeholder = { Text(stringResource(R.string.report_other_details_label)) },
                                minLines = 2,
                                maxLines = 3,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 8.dp)
                                    .fillMaxWidth()
                                    .heightIn(min = 88.dp)
                                    .onFocusChanged {
                                        if (it.isFocused) {
                                            selectedReasonName = ReportReason.OTHER.name
                                        }
                                    }
                            )
                        } else {
                            ReportReason.values()
                                .filter { it.group == group }
                                .forEach { reason ->
                                    ReportReasonOption(
                                        reason = reason,
                                        selected = selectedReason == reason,
                                        enabled = !isSubmitting,
                                        onClick = {
                                            selectedReasonName = reason.name
                                            otherDetails = ""
                                        }
                                    )
                                }
                        }
                    }

                    HorizontalDivider(color = TextLabelGray.copy(alpha = 0.16f))
                }
            }
        },
        containerColor = CardWhite,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ReportGroupHeader(
    group: ReportReasonGroup,
    expanded: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(group.labelResId),
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextLabelGray
        )
    }
}

@Composable
private fun ReportReasonOption(
    reason: ReportReason,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(selectedColor = StatPinkDark)
        )
        Text(
            text = stringResource(reason.labelResId),
            color = TextPrimary,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportDialogPreview() {
    FoodTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            ReportDialog(
                isSubmitting = false,
                onDismissRequest = {},
                onConfirmReport = { _, _ -> }
            )
        }
    }
}
