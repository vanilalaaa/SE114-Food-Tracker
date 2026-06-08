package com.SE114.food_tracker.feature.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun CurrencySelectionDialog(
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Đơn vị tiền",
                    style = AppTypography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CurrencyRadioItem(
                        currencyCode = "đ VND",
                        currencyName = "VIETNAMESE DONG",
                        isSelected = true,
                        onClick = { /* ViewModel lo */ }
                    )
                    CurrencyRadioItem(
                        currencyCode = "€ EUR",
                        currencyName = "EURO",
                        isSelected = false,
                        onClick = { /* ViewModel lo */ }
                    )
                    CurrencyRadioItem(
                        currencyCode = "¥ JPY",
                        currencyName = "JAPANESE YEN",
                        isSelected = false,
                        onClick = { /* ViewModel lo */ }
                    )
                    CurrencyRadioItem(
                        currencyCode = "$ USD",
                        currencyName = "US DOLLAR",
                        isSelected = false,
                        onClick = { /* ViewModel lo */ }
                    )
                    // Bổ sung Chinese Yen
                    CurrencyRadioItem(
                        currencyCode = "¥ CNY",
                        currencyName = "CHINESE YEN",
                        isSelected = false,
                        onClick = { /* ViewModel lo */ }
                    )
                    // Bổ sung Korean Won
                    CurrencyRadioItem(
                        currencyCode = "₩ KRW",
                        currencyName = "KOREAN WON",
                        isSelected = false,
                        onClick = { /* ViewModel lo */ }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                SettingActionButton(
                    text = "CHỌN",
                    onClick = onConfirmClick
                )
            }
        }
    }
}

@Preview
@Composable
fun CurrencySelectionDialogPreview() {
    FoodTrackerTheme {
        CurrencySelectionDialog(
            onDismissRequest = {},
            onConfirmClick = {}
        )
    }
}