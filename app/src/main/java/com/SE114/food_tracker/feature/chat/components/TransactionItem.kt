package com.SE114.food_tracker.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun TransactionItem(
    actorName: String,
    type: String, // 'deposit', 'withdrawal', 'purchase'
    amount: Double,
    note: String,
    createdAt: String,
    modifier: Modifier = Modifier
) {
    val isPositive = amount > 0

    val chipContainerColor = when (type) {
        "deposit" -> LightGreenStat
        "withdrawal" -> StatPinkLight
        else -> LightPeachStat
    }

    val chipTextColor = when (type) {
        "deposit" -> Color(0xFF2E7D32)
        "withdrawal" -> StatPinkDark
        else -> SettingActionOrange
    }

    val chipLabel = when (type) {
        "deposit" -> "Nộp tiền"
        "withdrawal" -> "Rút quỹ"
        else -> "Mua món"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = actorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )

                    Surface(
                        color = chipContainerColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = chipLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = chipTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(text = note, fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = createdAt, fontSize = 11.sp, color = HintGray)
            }

            Text(
                text = if (isPositive) "+${
                    String.format(
                        "%,.0f",
                        amount
                    )
                }" else String.format("%,.0f", amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) Color(0xFF2E7D32) else StatPinkDark
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionItemDepositPreview() {
    FoodTrackerTheme {
        Box(modifier = Modifier
            .background(MainBackground)
            .padding(16.dp)) {
            TransactionItem(
                actorName = "Thúy Vy",
                type = "deposit",
                amount = 200000.0,
                note = "Nộp tiền quỹ trưa tuần này nhe",
                createdAt = "10:15 - Hôm nay"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionItemPurchasePreview() {
    FoodTrackerTheme {
        Box(modifier = Modifier
            .background(MainBackground)
            .padding(16.dp)) {
            TransactionItem(
                actorName = "Azun (Data)",
                type = "purchase",
                amount = -125000.0,
                note = "Chi quỹ mua cơm tấm gà quay",
                createdAt = "12:05 - Hôm qua"
            )
        }
    }
}