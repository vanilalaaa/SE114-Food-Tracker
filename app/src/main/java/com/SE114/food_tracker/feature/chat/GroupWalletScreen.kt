package com.SE114.food_tracker.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.feature.chat.components.TransactionItem
import com.SE114.food_tracker.feature.chat.components.WalletTransactionDialog

data class GroupWalletUiTxModel(
    val id: String,
    val actorName: String,
    val type: String,
    val amount: Double,
    val note: String,
    val createdAt: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupWalletScreen(
    conversationId: String,
    onBackClick: () -> Unit = {}
) {
    var hasWallet by remember { mutableStateOf(true) }
    var walletBalance by remember { mutableStateOf(520000.0) }
    var walletName by remember { mutableStateOf("Quỹ Ăn Trưa Nhóm SE114 🥑") }

    var showTransactionDialog by remember { mutableStateOf<String?>(null) } // "deposit" hoặc "withdrawal"
    var selectedFilter by remember { mutableStateOf("Tất cả") }

    val mockTransactions = remember {
        listOf(
            GroupWalletUiTxModel(
                "1",
                "Thúy Vy",
                "deposit",
                200000.0,
                "Nộp tiền quỹ tuần mới",
                "10:15 - Hôm nay"
            ),
            GroupWalletUiTxModel(
                "2",
                "Azun",
                "purchase",
                -120000.0,
                "Mua 3 hộp cơm gà phi lê",
                "12:30 - Hôm qua"
            ),
            GroupWalletUiTxModel(
                "3",
                "Thẻo U",
                "deposit",
                500000.0,
                "Quỹ mồi lập nhóm",
                "09:00 - 15/06"
            ),
            GroupWalletUiTxModel(
                "4",
                "Tịnh Zan",
                "withdrawal",
                -60000.0,
                "Rút tiền trả ship trà sữa",
                "16:20 - 14/06"
            )
        )
    }

    val filteredTransactions = mockTransactions.filter {
        when (selectedFilter) {
            "Nộp tiền" -> it.type == "deposit"
            "Chi tiêu" -> it.type == "withdrawal" || it.type == "purchase"
            else -> true
        }
    }

    if (showTransactionDialog != null) {
        WalletTransactionDialog(
            transactionType = showTransactionDialog!!,
            onDismissRequest = { showTransactionDialog = null },
            onConfirm = { amount, note ->
                if (showTransactionDialog == "deposit") {
                    walletBalance += amount
                } else if (showTransactionDialog == "withdrawal" && walletBalance >= amount) {
                    walletBalance -= amount
                }
                showTransactionDialog = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Quỹ Nhóm Tài Chính",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MainBackground)
            )
        },
        containerColor = MainBackground
    ) { innerPadding ->

        if (!hasWallet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("💰", fontSize = 48.sp)
                        Text(
                            text = "Cuộc trò chuyện này chưa được thiết lập Quỹ nhóm",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = TextSecondary
                        )
                        var newWalletName by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = newWalletName,
                            onValueChange = { newWalletName = it },
                            placeholder = { Text("Nhập tên Quỹ nhóm...", color = HintGray) },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StatPinkDark,
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (newWalletName.isNotBlank()) {
                                    walletName = newWalletName
                                    hasWallet = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatPinkDark),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Tạo Quỹ nhóm mới (Chỉ Admin)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = walletName,
                            fontSize = 14.sp,
                            color = TextLabelGray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${String.format("%,.0f", walletBalance)} VND",
                            fontSize = 28.sp, fontWeight = FontWeight.Bold, color = StatPinkDark
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showTransactionDialog = "deposit" },
                                colors = ButtonDefaults.buttonColors(containerColor = LightGreenStat),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text(
                                    "📥 Nộp tiền",
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = { showTransactionDialog = "withdrawal" },
                                colors = ButtonDefaults.buttonColors(containerColor = StatPinkLight),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text(
                                    "📤 Rút quỹ",
                                    color = StatPinkDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Tất cả", "Nộp tiền", "Chi tiêu").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            shape = RoundedCornerShape(24.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StatPinkLight,
                                selectedLabelColor = StatPinkDark,
                                disabledContainerColor = CardWhite,
                                disabledLabelColor = HintGray
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == filter,
                                borderColor = Color(0xFFE0E0E0),
                                selectedBorderColor = StatPinkDark,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp
                            )
                        )
                    }
                }

                Text(
                    "Lịch sử giao dịch quỹ (Mới nhất)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLabelGray
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        TransactionItem(
                            actorName = tx.actorName,
                            type = tx.type,
                            amount = tx.amount,
                            note = tx.note,
                            createdAt = tx.createdAt
                        )
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun GroupWalletScreenPreview() {
    FoodTrackerTheme {
        GroupWalletScreen(conversationId = "2")
    }
}