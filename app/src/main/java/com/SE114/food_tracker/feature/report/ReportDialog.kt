package com.SE114.food_tracker.feature.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onConfirmReport: (ReportReason) -> Unit
) {
    var selectedReason by remember { mutableStateOf(ReportReason.SPAM) }

    AlertDialog(
        onDismissRequest = {
            if (!isSubmitting) onDismissRequest()
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReport(selectedReason) },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = StatPinkDark),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = if (isSubmitting) "Đang gửi..." else "Gửi báo cáo",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !isSubmitting
            ) {
                Text("Hủy", color = TextLabelGray)
            }
        },
        title = {
            Text(
                text = "Báo cáo người dùng",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Chọn lý do báo cáo:",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                ReportReason.values().forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSubmitting) {
                                selectedReason = reason
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            enabled = !isSubmitting,
                            colors = RadioButtonDefaults.colors(selectedColor = StatPinkDark)
                        )
                        Text(
                            text = reason.label,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        containerColor = CardWhite,
        shape = RoundedCornerShape(24.dp)
    )
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
                onConfirmReport = {}
            )
        }
    }
}
