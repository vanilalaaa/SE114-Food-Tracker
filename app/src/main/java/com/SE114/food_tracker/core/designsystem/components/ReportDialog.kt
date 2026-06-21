package com.SE114.food_tracker.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDialog(
    isAlreadyReportedBefore: Boolean, // Nhận trạng thái cảnh báo trùng từ ViewModel
    onDismissRequest: () -> Unit,
    onConfirmReport: (reason: String, note: String) -> Unit
) {
    val reasons = listOf("Spam", "Nội dung không phù hợp", "Quấy rối", "Khác")
    var selectedReason by remember { mutableStateOf(reasons[0]) }
    var noteInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = { onConfirmReport(selectedReason, noteInput) },
                colors = ButtonDefaults.buttonColors(containerColor = StatPinkDark),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Gửi báo cáo", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Hủy", color = TextLabelGray)
            }
        },
        title = {
            Text("Báo cáo nội dung vi phạm", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hiện cảnh báo nếu đã từng report mục tiêu này
                if (isAlreadyReportedBefore) {
                    Surface(
                        color = StatPinkLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ Lưu ý: Bạn đã từng gửi báo cáo cho nội dung này rồi.",
                            color = StatPinkDark,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Text("Chọn lý do báo cáo:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)

                // Danh sách Radio Buttons chọn lý do
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedReason == reason),
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = StatPinkDark)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = reason, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Ghi chú bổ sung (nếu có):", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    placeholder = { Text("Nhập thêm chi tiết vi phạm...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StatPinkDark,
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    ),
                    maxLines = 3
                )
            }
        },
        containerColor = CardWhite,
        shape = RoundedCornerShape(24.dp)
    )
}

@Preview(name = "Báo cáo lần đầu - Trạng thái sạch", showBackground = true)
@Composable
fun ReportDialogNormalPreview() {
    FoodTrackerTheme {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            ReportDialog(
                isAlreadyReportedBefore = false,
                onDismissRequest = {},
                onConfirmReport = { _, _ -> }
            )
        }
    }
}

@Preview(name = "Báo cáo trùng - Hiện cảnh báo hồng", showBackground = true)
@Composable
fun ReportDialogDuplicateWarningPreview() {
    FoodTrackerTheme {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            ReportDialog(
                isAlreadyReportedBefore = true,
                onDismissRequest = {},
                onConfirmReport = { _, _ -> }
            )
        }
    }
}