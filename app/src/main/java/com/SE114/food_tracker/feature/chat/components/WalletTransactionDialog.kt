package com.SE114.food_tracker.feature.chat.components

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

@Composable
fun WalletTransactionDialog(
    transactionType: String, // "deposit" hoặc "withdrawal"
    onDismissRequest: () -> Unit,
    onConfirm: (amount: Double, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = LightPinkBG,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (transactionType == "deposit") "📥 Nộp tiền vào Quỹ" else "📤 Rút tiền từ Quỹ",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = StatPinkDark
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Số tiền (VND)", color = TextLabelGray) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardWhite,
                        unfocusedContainerColor = CardWhite,
                        focusedBorderColor = StatPinkDark,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedLabelColor = StatPinkDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Ghi chú hoạt động", color = TextLabelGray) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardWhite,
                        unfocusedContainerColor = CardWhite,
                        focusedBorderColor = StatPinkDark,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedLabelColor = StatPinkDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
                    if (parsedAmount > 0) {
                        onConfirm(parsedAmount, noteInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatPinkDark), // Màu hồng trầm rực rỡ nịnh mắt
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Xác nhận", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Hủy", color = TextLabelGray, fontWeight = FontWeight.SemiBold)
            }
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun WalletTransactionDialogDepositPreview() {
    FoodTrackerTheme {
        WalletTransactionDialog(
            transactionType = "deposit",
            onDismissRequest = {},
            onConfirm = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WalletTransactionDialogWithdrawalPreview() {
    FoodTrackerTheme {
        WalletTransactionDialog(
            transactionType = "withdrawal",
            onDismissRequest = {},
            onConfirm = { _, _ -> }
        )
    }
}